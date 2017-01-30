package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.Future
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import com.softwaremill.quicklens._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{Attachment, S3, S3ServerEmbedded}
import uk.gov.homeoffice.aws.sqs.SQSServerEmbedded
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercurySpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  trait Context extends SQSServerEmbedded with S3ServerEmbedded

  "Mercury" should {
    val `text/plain`: String = akka.http.scaladsl.model.MediaTypes.`text/plain`

    "publish an AWS SQS message" in new Context {
      routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
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
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
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
      val file = new File(s"$s3Directory/test-file.txt")
      val `file-name` = file.getName

      routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
          println(s"===> ${request.body.files.size}")

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

        val file = new File(s"$s3Directory/test-file.txt")

        val message = createMessage("A plain text message").modify(_.sqsMessage)
          .using(_.addAttributesEntry("key", "test-file.txt").addAttributesEntry("fileName", "test-file.txt").addAttributesEntry("contentType", `text/plain`))
          //.using(_.addAttributesEntry("key.1", "test-file.txt").addAttributesEntry("fileName.1", "test-file.txt").addAttributesEntry("contentType.1", `text/plain`))

        mercury.s3.push(file.getName, file) flatMap { _ =>
          mercury publish message
        } must beEqualTo("caseRef").await
      }
    }
  }

  "Mercury S3" should {
    "pull in one file from S3" in new Context {
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
  }
}