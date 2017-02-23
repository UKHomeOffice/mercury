package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.stream.scaladsl.{Source, StreamConverters}
import play.api.http.Status._
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.web.WebService

object Mercury {
  val authorizationEndpoint = "/alfresco/s/api/login"

  val publicationEndpoint = "/alfresco/s/homeoffice/cts/autoCreateDocument"

  def authorize(credentials: Credentials)(implicit webService: WebService): Future[WebService with Authorization] = {
    webService endpoint authorizationEndpoint post credentials flatMap { response =>
      response.status match {
        case OK => Future successful new WebService(webService.host, webService.wsClient) with Authorization {
          override val token: String = (response.json \ "data" \ "ticket").as[String]
        }

        case _ => Future failed new Exception(s"""Failed to authorize against "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
      }
    }
  }

  def apply(s3: S3, webService: WebService with Authorization) = new Mercury(s3, webService)
}

class Mercury(val s3: S3, val webService: WebService with Authorization) extends Logging {
  import Mercury._

  lazy val authorizationParam = "alf_ticket" -> webService.token

  val publish: Message => Future[Publication] = { m =>
    info(m)

    val folder = m.content + (if (m.content.endsWith("/")) "" else "/")
    val FileNameRegex = s"$folder(.*)".r

    s3 pullResources folder flatMap {
      case Nil =>
        Future failed new Exception(s"""No existing resources on S3 for given SQS event "${m.content}"""")

      case resources =>
        val numberOfFileParts = if (resources.size == 1) "1 resource" else s"${resources.size} resources"
        info(s"""Publishing to endpoint ${webService.host}$publicationEndpoint, $numberOfFileParts associated with S3 key ${m.content}""")

        val fileParts = resources map {
          case Resource(key, inputStream, contentType, _) =>
            val data = StreamConverters.fromInputStream(() => inputStream)
            val FileNameRegex(fileName) = key
            FilePart("file", fileName, Some(contentType), data) // TODO Key? Name of file? The type?
        }

        // TODO Case type, name are hardcoded
        webService endpoint publicationEndpoint withQueryString authorizationParam post Source(List(DataPart("caseType", "IMCB"), DataPart("name", "email.txt")) ++ fileParts) flatMap { response =>
          response.status match {
            case OK =>
              // TODO delete "folder"
              Future successful Publication("caseRef") // TODO

            case _ =>
              throw new Exception(s"""Failed to publish to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
          }
        }
    }
  }
}