package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import akka.stream.scaladsl.{Source, StreamConverters}
import play.api.http.Status._
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import org.json4s.DefaultFormats
import org.json4s.jackson.parseJson
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3.S3.ResourceKey
import uk.gov.homeoffice.aws.s3._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.configuration.HasConfig
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

class Mercury(val s3: S3, val webService: WebService with Authorization) extends HasConfig with Logging {
  import Mercury._

  lazy val authorizationParam = "alf_ticket" -> webService.token

  val parse: Message => ResourceKey = { message =>
    implicit val formats = DefaultFormats

    ((parseJson(message.content) \ "Records")(0) \ "s3" \ "object" \ "key").extract[String]
  }

  val publish: Message => Future[Publication] = { message =>
    info(message)

    s3 pullResource parse(message) flatMap { resource =>
      val data = StreamConverters.fromInputStream(() => resource.inputStream)
      val fileName = resource.key.substring(resource.key.lastIndexOf("/") + 1)
      val resourceFilePart = FilePart("file", fileName, Some(resource.contentType), data) // TODO Key? Name of file? The type?

      info(s"""Publishing to endpoint ${webService.host}$publicationEndpoint, resource with S3 key "${resource.key}"""")

      // TODO Case type, name are hardcoded
      webService endpoint publicationEndpoint withQueryString authorizationParam post Source(List(DataPart("caseType", "IMCB"), DataPart("name", "email.txt"), resourceFilePart)) map { response =>
        response.status match {
          case OK =>
            // TODO delete "resource"
            Publication("caseRef") // TODO

          case _ =>
            throw new Exception(s"""Failed to publish to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
        }
      }
    }
  }
}