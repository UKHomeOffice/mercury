package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.duration._
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import com.softwaremill.quicklens._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.WebServiceSpecification

class MercuryPublicationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  val `text/plain`: String = akka.http.scaladsl.model.MediaTypes.`text/plain`

  "Mercury" should {
    "publish an AWS SQS message" in new MercuryServicesContext {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", "email.txt", Some(`text/plain`), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val result = Mercury authorize credentials flatMap { webService =>
          val mercury = Mercury(mock[S3], webService)
          mercury publish createMessage("A plain text message")
        }

        result must beEqualTo("caseRef").awaitFor(5.seconds)
      }
    }

    "fail to publish an AWS SQS message because it is not authorized" in new MercuryServicesContext {
      routes(authorizeRoute orElse authorizeCheck) { implicit ws =>
        val result = Mercury authorize credentials flatMap { webService =>
          val mercury = new Mercury(mock[S3], webService) {
            override lazy val authorizationParam = "" -> ""
          }

          mercury publish createMessage("A plain text message")
        }

        result must throwAn[Exception](message = "401, Unauthorized").awaitFor(5.seconds)
      }
    }

    "fail to publish an AWS SQS message because endpoint is not available" in new MercuryServicesContext {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case _ => Action(BadGateway)
      }) { implicit ws =>
        val result = Mercury authorize credentials flatMap { webService =>
          val mercury = Mercury(mock[S3], webService)
          mercury publish createMessage("A plain text message")
        }

        result must throwAn[Exception](message = "502, Bad Gateway").awaitFor(5.seconds)
      }
    }

    "publish an AWS SQS message converted to PDF" in new MercuryServicesContext {
      todo

      /*routes {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some("text/plain"), tempFile)) = request.body.files
          Ok
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must beEqualTo("caseRef").awaitFor(5.seconds)
      }*/
    }

    "publish an AWS SQS message with an associated attachment" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val `file-name` = file.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain and a second (attachment) of type text/plain
          val Seq(FilePart("file", "email.txt", Some(`text/plain`), emailFile),
          FilePart("file", `file-name`, Some(`text/plain`), attachmentFile)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val result = Mercury authorize credentials flatMap { webService =>
          val mercury = Mercury(new S3("test-bucket"), webService)

          val message = createMessage("A plain text message")
            .modify(_.sqsMessage)
            .using(_.addAttributesEntry("key", "test-file.txt").addAttributesEntry("fileName", "test-file.txt").addAttributesEntry("contentType", `text/plain`))

          mercury.s3.push(file.getName, file) flatMap { _ =>
            mercury publish message
          }
        }

        result must beEqualTo("caseRef").awaitFor(5.seconds)
      }
    }
  }

  "publish an AWS SQS message with two associated attachments" in new MercuryServicesContext {
    val file1 = new File(s"$s3Directory/test-file-1.txt")
    val `file-name-1` = file1.getName

    val file2 = new File(s"$s3Directory/test-file-2.txt")
    val `file-name-2` = file2.getName

    routes(authorizeRoute orElse authorizeCheck orElse {
      case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
        // Expect one file of type text/plain, a second (attachment) of type text/plain and third (attachment) of type text/plain
        val Seq(FilePart("file", "email.txt", Some(`text/plain`), _),
        FilePart("file", `file-name-1`, Some(`text/plain`), _),
        FilePart("file", `file-name-2`, Some(`text/plain`), _)) = request.body.files
        Ok
      }
    }) { implicit ws =>
      val result = Mercury authorize credentials flatMap { webService =>
        val mercury = Mercury(new S3("test-bucket"), webService)

        val message = createMessage("A plain text message")
          .modify(_.sqsMessage)
          .using(_.addAttributesEntry("key.1", "test-file-1.txt").addAttributesEntry("fileName.1", "test-file-1.txt").addAttributesEntry("contentType.1", `text/plain`)
                  .addAttributesEntry("key.2", "test-file-2.txt").addAttributesEntry("fileName.2", "test-file-2.txt").addAttributesEntry("contentType.2", `text/plain`))

        for {
          _ <- mercury.s3.push(file1.getName, file1)
          _ <- mercury.s3.push(file2.getName, file2)
          caseRef <- mercury publish message
        } yield caseRef
      }

      result must beEqualTo("caseRef").awaitFor(5.seconds)
    }
  }
}