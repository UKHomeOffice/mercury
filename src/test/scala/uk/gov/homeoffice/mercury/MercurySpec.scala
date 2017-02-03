package uk.gov.homeoffice.mercury

import java.io.File
import java.net.URL
import scala.concurrent.Future
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Handler, RequestHeader}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server
import com.softwaremill.quicklens._
import org.json4s.JsonAST.JObject
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{Specification, SpecificationLike}
import uk.gov.homeoffice.aws.s3.{Attachment, S3, S3ServerEmbedded}
import uk.gov.homeoffice.aws.sqs.SQSServerEmbedded
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercurySpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  trait Context extends SQSServerEmbedded with S3ServerEmbedded

  val userNameLogin = "emailapiuser"
  val passwordLogin = "Password1"

  val loginRoute: PartialFunction[RequestHeader, Handler] = {
    case POST(p"/alfresco/s/api/login") => Action(parse.json) { request =>
      val userName = (request.body \ "username").asOpt[String]
      val password = (request.body \ "password").asOpt[String]

      (userName, password) match {
        case (Some(`userNameLogin`), Some(`passwordLogin`)) => Ok(Json.obj("data" -> Json.obj("ticket" -> "TICKET_1")))
        case _ => Unauthorized
      }
    }
  }

  "Mercury authorization" should {
    "fail because of missing user name" in new Context {
      routes(loginRoute) { implicit ws =>
        /*Mercury authorize Login("", loginValid.password) flatMap { webService =>
          val mercury = Mercury(mock[S3], webService)

          mercury publish createMessage("A plain text message")

        } must throwAn[Exception](message = "401, Unauthorized").await*/

        Mercury authorize Login("", passwordLogin) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail because of invalid user name" in new Context {
      routes(loginRoute) { implicit ws =>
        Mercury authorize Login("wrong", passwordLogin) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    /*"pass and be given a token" in new Context {
      skipped

      routes {
        case POST(p"/alfresco/s/api/login") => Action(parse.json) { request =>
          val userName = (request.body \ "username").asOpt[String]
          val password = (request.body \ "password").asOpt[String]

          (userName, password) match {
            case (Some(u), Some(p)) => Ok(Json.obj("data" -> Json.obj("ticket" -> "TICKET_1")))
            case _ => Unauthorized
          }
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }*/
  }

  /*"Mercury" should {
    val `text/plain`: String = akka.http.scaladsl.model.MediaTypes.`text/plain`

    "fail to publish an AWS SQS message because it is not authorized" in new Context {
      routes {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          Unauthorized
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail to publish an AWS SQS message because it cannot be authorized when missing required authorized token" in new Context {
      val mercury = new Mercury {
        val s3 = mock[S3]
        val webService = mock[WebService]
      }

      mercury publish createMessage("A plain text message") must throwAn[Exception](message = "401, Unauthorized").await
    }

    "publish an AWS SQS message" in new Context {
      skipped

      routes {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some(`text/plain`), _)) = request.body.files
          Ok
        }
      } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must beEqualTo("caseRef").await
      }
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

  "Mercury S3" should {
    "pull in one file from S3" in new Context {
      skipped

      val mercury = new Mercury {
        val s3 = new S3("test-bucket")
        val webService = mock[WebService]
      }

      val file = new File(s"$s3Directory/test-file.txt")

      val pulledFiles = mercury.s3.push(file.getName, file) flatMap { _ =>
        mercury pull Seq(Attachment(file.getName, file.getName, `text/plain`))
      }

      pulledFiles must beLike[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] {
        case Seq(f) => f.key mustEqual file.getName
      }.await
    }

    "pull in two files from S3" in new Context {
      skipped

      val mercury = new Mercury {
        val s3 = new S3("test-bucket")
        val webService = mock[WebService]
      }

      val file1 = new File(s"$s3Directory/test-file.txt")
      val file2 = new File(s"$s3Directory/test-file-2.txt")

      val pulledFiles = for {
        _ <- mercury.s3.push(file1.getName, file1)
        _ <- mercury.s3.push(file2.getName, file2)
        files <- mercury pull Seq(Attachment(file1.getName, file1.getName, `text/plain`),
                                  Attachment(file2.getName, file2.getName, `text/plain`))
      } yield files

      pulledFiles must beLike[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] {
        case Seq(f1, f2) =>
          f1.key mustEqual file1.getName
          f2.key mustEqual file2.getName
      }.await
    }
  }*/
}