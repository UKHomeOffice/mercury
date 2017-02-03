package uk.gov.homeoffice.mercury

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.mvc.{Action, Handler, Request, RequestHeader}
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{S3, S3ServerEmbedded}
import uk.gov.homeoffice.aws.sqs.SQSServerEmbedded
import uk.gov.homeoffice.web.WebServiceSpecification
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._

class MercuryPublicationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  "Mercury" should {
    val `text/plain`: String = akka.http.scaladsl.model.MediaTypes.`text/plain`

    "publish an AWS SQS message" in new MercuryServicesContext {
      routes(loginRoute orElse loginCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some(`text/plain`), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>

        Mercury authorize login flatMap { webService =>
          val mercury = Mercury(mock[S3], webService)
          mercury publish createMessage("A plain text message")

        } must beEqualTo("caseRef").await
      }
    }

    "fail to publish an AWS SQS message because it is not authorized" in new MercuryServicesContext {
      routes(loginRoute orElse loginCheck) { implicit ws =>

        Mercury authorize login flatMap { webService =>
          val mercury = new Mercury(mock[S3], webService) {
            override val authorizationParam: (String, String) = "" -> ""
          }

          mercury publish createMessage("A plain text message")

        } must throwAn[Exception](message = "401, Unauthorized").await
      }
    }
  }


  /*


    "fail to publish an AWS SQS message because it cannot be authorized when missing required authorized token" in new Context {
      val mercury = new Mercury {
        val s3 = mock[S3]
        val webService = mock[WebService]
      }

      mercury publish createMessage("A plain text message") must throwAn[Exception](message = "401, Unauthorized").await
    }



    "fail to publish an AWS SQS message because endpoint is not available" in new Context {
      skipped

      routes { case _ => Action(BadGateway) } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must throwAn[Exception](message = "502, Bad Gateway").await
      }
    }

    "publish an AWS SQS message converted to PDF" in new Context {
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

        mercury publish createMessage("A plain text message") must beEqualTo("caseRef").await
      }*/
    }

    "publish an AWS SQS message with an associated attachment" in new Context {
      skipped

      val file = new File(s"$s3Directory/test-file.txt")
      val `file-name` = file.getName

      routes {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain and a second (attachment) of type text/plain
          val Seq(FilePart("email", "email.txt", Some(`text/plain`), emailFile),
                  FilePart(`file-name`, `file-name`, Some(`text/plain`), attachmentFile)) = request.body.files
          Ok
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = new S3("test-bucket")
          val webService = ws
        }

        val message = createMessage("A plain text message").modify(_.sqsMessage)
          .using(_.addAttributesEntry("key", "test-file.txt").addAttributesEntry("fileName", "test-file.txt").addAttributesEntry("contentType", `text/plain`))

        mercury.s3.push(file.getName, file) flatMap { _ =>
          mercury publish message
        } must beEqualTo("caseRef").await
      }
    }

    "publish an AWS SQS message with two associated attachments" in new Context {
      skipped

      val file1 = new File(s"$s3Directory/test-file-1.txt")
      val `file-name-1` = file1.getName

      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val `file-name-2` = file2.getName

      routes {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain, a second (attachment) of type text/plain and third (attachment) of type text/plain
          val Seq(FilePart("email", "email.txt", Some(`text/plain`), _),
                  FilePart(`file-name-1`, `file-name-1`, Some(`text/plain`), _),
                  FilePart(`file-name-2`, `file-name-2`, Some(`text/plain`), _)) = request.body.files
          Ok
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = new S3("test-bucket")
          val webService = ws
        }

        val message = createMessage("A plain text message").modify(_.sqsMessage)
          .using(_.addAttributesEntry("key.1", "test-file-1.txt").addAttributesEntry("fileName.1", "test-file-1.txt").addAttributesEntry("contentType.1", `text/plain`)
                  .addAttributesEntry("key.2", "test-file-2.txt").addAttributesEntry("fileName.2", "test-file-2.txt").addAttributesEntry("contentType.2", `text/plain`))

        (for {
          _ <- mercury.s3.push(file1.getName, file1)
          _ <- mercury.s3.push(file2.getName, file2)
          caseRef <- mercury publish message
        } yield caseRef) must beEqualTo("caseRef").await
      }
    }
  }
*/
}