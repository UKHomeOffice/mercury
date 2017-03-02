package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.stream.scaladsl.{Source, StreamConverters}
import play.api.http.Status._
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import org.json4s.DefaultFormats
import org.json4s.jackson.parseJson
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3.S3.ResourcesKey
import uk.gov.homeoffice.aws.s3._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.web.WebService

object Mercury {
  type Key = String

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

  val parse: Message => Key = { m =>
    implicit val formats = DefaultFormats

    ((parseJson(m.content) \ "Records")(0) \ "s3" \ "bucket" \ "object" \ "key").extract[String]
  }

  val publish: Message => Future[Seq[Publication]] = { m =>
    info(m)

    s3.pullResources(groupByTopDirectory _).flatMap { pulledResources =>
      val publications = pulledResources.toSeq.map { case (resourcesKey, resources) =>
        val fileParts = resources map {
          case Resource(key, inputStream, contentType, _, _) =>
            val data = StreamConverters.fromInputStream(() => inputStream)
            val fileName = key.substring(key.lastIndexOf("/") + 1)
            FilePart("file", fileName, Some(contentType), data) // TODO Key? Name of file? The type?
        }

        val numberOfFileParts = if (resources.size == 1) "1 resource" else s"${resources.size} resources"
        info(s"""Publishing to endpoint ${webService.host}$publicationEndpoint, $numberOfFileParts associated with S3 key $resourcesKey""")

        // TODO Case type, name are hardcoded
        webService endpoint publicationEndpoint withQueryString authorizationParam post Source(List(DataPart("caseType", "IMCB"), DataPart("name", "email.txt")) ++ fileParts) map { response =>
          response.status match {
            case OK =>
              // TODO delete "folder"
              Publication("caseRef") // TODO

            case _ =>
              throw new Exception(s"""Failed to publish to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
          }
        }
      }

      Future sequence publications
    }
  }

  def groupByTopDirectory(resources: Seq[Resource]): Map[ResourcesKey, Seq[Resource]] = resources.groupBy { resource =>
    resource.key.indexOf("/") match {
      case -1 => resource.key
      case i => resource.key.take(i)
    }
  }
}