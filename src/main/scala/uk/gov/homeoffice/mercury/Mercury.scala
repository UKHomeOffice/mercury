package uk.gov.homeoffice.mercury

import java.io.FileInputStream
import java.util.Random

import akka.http.scaladsl.model.MediaTypes._
import akka.stream.scaladsl.{Source, StreamConverters}
import grizzled.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.parseJson
import play.api.http.Status._
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.homeoffice.aws.s3.S3.ResourceKey
import uk.gov.homeoffice.aws.s3._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.json.Json._
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.mercury.email.{EmailAttachment, EmailParser}
import uk.gov.homeoffice.mercury.pdf.EmailPdfGenerator
import uk.gov.homeoffice.web.WebService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

object Mercury {
  val authorizationEndpoint = "/alfresco/s/api/login"

  val publicationEndpoint = "/alfresco/s/homeoffice/ctsv2/createCase"

  val attachmentEndpoint = "/alfresco/s/homeoffice/cts/document"

  def authorize(credentials: Credentials)(implicit webService: WebService): Future[WebService with Authorization] = {
    webService endpoint authorizationEndpoint post credentials flatMap { response =>
      response.status match {
        case OK => Future successful new WebService(webService.host, webService.wsClient) with Authorization {
          override val token: String = (response.json \ "data" \ "ticket").as[String]
        }

        case _ => Future failed new Exception(s"""Failed to authorize against "${webService.host}"$authorizationEndpoint because of: Http response status ${response.status}, ${response.statusText}""")
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

    ((parseJson(message.content) \ "Records") (0) \ "s3" \ "object" \ "key").extract[String]
  }

  /**
    * Publish a resource from S3 according to a "key" within a given Message
    *
    * @param message Message Containing a "key' to be used to acquire a resource from S3 and publish
    * @return Future[Publication]
    */
  def publish(message: Message): Future[Publication] = {
    info(message)

    s3 pullResource parse(message) flatMap { resource =>
      processEmail(resource)
    } recoverWith {
      case ex =>
        error(ex.getMessage, ex)
        Future.failed(ex)
    }
  }

  private def processEmail(resource: Resource): Future[Publication] = {

    // Sleep 5 seconds because alfresco is proper shite.
    Thread.sleep(5000)

    //Parse email from raw text
    val email = EmailParser.parse(resource.inputStream)
    resource.inputStream.close()
    //Generate pdf file with email contents
    val pdf = EmailPdfGenerator.generatePdf(email)

    val data = StreamConverters.fromInputStream(() => new FileInputStream(pdf))
    val filePart = FilePart("file", pdf.getName, Some(`application/pdf`), data)

    val caseType = email.to.substring(0, email.to.indexOf("@")).toUpperCase

    info(s"""Publishing Case type $caseType to endpoint ${webService.host}$publicationEndpoint, resource with S3 key "${resource.key}"""")

    webService endpoint publicationEndpoint withQueryString authorizationParam post
      Source(
        List(
            DataPart("caseType", caseType),
            DataPart("fromEmail", email.from),
            DataPart("numberFiles", email.attachments.size.toString),
            filePart)
    ) map { response =>
      response.status match {
        case OK =>
          info(s"Published resource with associated S3 key ${resource.key}")

          s3.s3Client.deleteObject(s3.bucket, resource.key)

          val caseRef = (response.json \ "caseRef").as[String]

          email.attachments.foreach(addAttachment(caseRef, _))

          Publication(Try(response.json).map(jValue).getOrElse(JNothing))

        case INTERNAL_SERVER_ERROR =>
          //If the case type (i.e. the email recipient) is invalid then just log it
          if (response.body.contains(s"The value is not an allowed value: $caseType")) {
            error(s"""Failed to publish to "${webService.host}$publicationEndpoint" because the Case type $caseType is invalid""")
            s3.s3Client.deleteObject(s3.bucket, resource.key)
            Publication(JNothing)
          } else {
            throw new Exception(s"""Failed to publish to "${webService.host}$publicationEndpoint" because of: Http response status ${response.status}, ${response.statusText} with body:\n${response.body}""")
          }

        case _ =>
          throw new Exception(s"""Failed to publish to "${webService.host}$publicationEndpoint" because of: Http response status ${response.status}, ${response.statusText} with body:\n${response.body}""")
      }
    } andThen { case _ =>
      pdf.delete()
    }
  }

  private def addAttachment(caseRef: String, emailAttachment: EmailAttachment): Future[Unit] = {

    // Sleep 5 seconds because alfresco is proper shite.
    Thread.sleep(5000)

    val data = StreamConverters.fromInputStream(() => emailAttachment.body.getInputStream)
    val filePart = FilePart("file", emailAttachment.name, Some(emailAttachment.contentType), data)

    webService endpoint attachmentEndpoint withQueryString authorizationParam post Source(List(
      DataPart("name", emailAttachment.name), DataPart("destination", caseRef),
      DataPart("documenttype", "Original"), DataPart("documentdescription", "Email attachment"), filePart)
    ) map { response =>
      response.status match {
        case OK => {}
        case _ =>
          error(s"""Failed to publish attachment to "${webService.host}$attachmentEndpoint" because of: Http response status ${response.status}, ${response.statusText} with body:\n${response.body}""")
      }
    }
  }
}