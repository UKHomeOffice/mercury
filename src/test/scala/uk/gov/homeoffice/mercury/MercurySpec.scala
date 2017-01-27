package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.Future
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{S3, S3ServerEmbedded}
import uk.gov.homeoffice.aws.sqs.SQSServerEmbedded
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercurySpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  trait Context extends SQSServerEmbedded with S3ServerEmbedded

  "Mercury" should {
    "publish a translated plain text AWS SQS message" in new Context {
      routes {
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
      }
    }

    "fail to publish a translated plain text AWS SQS message because endpoint is not available" in new Context {
      routes { case _ => Action(BadGateway) } { ws =>
        val mercury = new Mercury {
          val s3 = mock[S3]
          val webService = ws
        }

        mercury publish createMessage("A plain text message") must throwAn[Exception](message = "502, Bad Gateway").await
      }
    }

    "publish a translated plain text AWS SQS message with an associated attachment" in new Context {
      todo

      /*routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
          // I expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some("text/plain"), tempFile)) = request.body.files
          Ok
        }
      } { webService =>
        val message = "A plain text message"

        val mercuryActor = TestActorRef {
          new MercuryActor(new Subscriber(queue), webService)
        }

        mercuryActor.underlyingActor publish createMessage(message) must beEqualTo("caseRef").await
      }*/
    }
  }

  "pull in one file from S3" in new Context {
    val mercury = new Mercury {
      val s3 = new S3("test-bucket")
      val webService = mock[WebService]
    }

    val file = new File(s"$s3Directory/test-file.txt")

    val pulledFiles = mercury.s3.push(file.getName, file) flatMap { _ =>
      mercury pull Seq(file.getName)
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
      pfs <- mercury pull Seq(file1.getName, file2.getName)
    } yield pfs

    pulledFiles must beLike[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] {
      case Seq(f1, f2) =>
        f1.key mustEqual file1.getName
        f2.key mustEqual file2.getName
    }.await
  }
}