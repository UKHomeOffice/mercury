package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try
import akka.stream.scaladsl.{Source, StreamConverters}
import play.api.http.Status._
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.parseJson
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3.S3.ResourceKey
import uk.gov.homeoffice.aws.s3._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.json.Json._
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

  /**
    * Publish a resource from S3 according to a "key" within a given Message
    * @param message Message Containing a "key' to be used to acquire a resource from S3 and publish
    * @return Future[Publication]
    */
  def publish(message: Message): Future[Publication] = {
    info(message)

    s3 pullResource parse(message) flatMap { resource =>
      val data = StreamConverters.fromInputStream(() => resource.inputStream)
      val fileName = resource.key.substring(resource.key.lastIndexOf("/") + 1)
      val resourceFilePart = FilePart("file", fileName, Some(resource.contentType), data)

      info(s"""Publishing to endpoint ${webService.host}$publicationEndpoint, resource with S3 key "${resource.key}"""")

      // TODO Case type, name are hardcoded
      webService endpoint publicationEndpoint withQueryString authorizationParam post Source(List(DataPart("caseType", "IMCB"), DataPart("name", "email.txt"), resourceFilePart)) map { response =>
        response.status match {
          case OK =>
            s3.s3Client.deleteObject(s3.bucket, resource.key)
            Publication(Try(response.json).map(jValue).getOrElse(JNothing))

          case _ =>
            throw new Exception(s"""Failed to publish to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
        }
      }
    }
  }

  /**
    * Publish all available resources on S3
    * @return Future[Seq[Publication]
    */
  def publish: Future[Seq[Publication]] = {
    info("Publishing...")

    s3.pullResources().flatMap { pulledResources =>
      if (pulledResources.isEmpty) info("No available resources to publish")

      val publications = pulledResources.toSeq.map { case (resourcesKey, resources) =>
        val fileParts = resources map {
          case Resource(key, inputStream, contentType, _, _) =>
            val data = StreamConverters.fromInputStream(() => inputStream)
            val fileName = key.substring(key.lastIndexOf("/") + 1)
            FilePart("file", fileName, Some(contentType), data)
        }

        val numberOfFileParts = if (resources.size == 1) "1 resource" else s"${resources.size} resources"
        info(s"""Publishing to endpoint ${webService.host}$publicationEndpoint, $numberOfFileParts associated with S3 key $resourcesKey""")

        // TODO Case type, name are hardcoded
        webService endpoint publicationEndpoint withQueryString authorizationParam post Source(List(DataPart("caseType", "IMCB"), DataPart("name", "email.txt")) ++ fileParts) map { response =>
          response.status match {
            case OK =>
              info(s"Published resource with associated S3 key $resourcesKey")

              resources foreach { resource =>
                s3.s3Client.deleteObject(s3.bucket, resource.key)
              }

              Publication(Try(response.json).map(jValue).getOrElse(JNothing))

            case _ =>
              throw new Exception(s"""Failed to publish to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText} with body:\n${response.body}""")
          }
        }
      }

      Future sequence publications
    }
  }
}