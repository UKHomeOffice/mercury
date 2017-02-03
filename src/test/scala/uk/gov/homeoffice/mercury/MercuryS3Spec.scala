package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.Future
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData.FilePart
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{Attachment, S3, S3ServerEmbedded}
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryS3Spec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  trait Context extends S3ServerEmbedded {
    val mercury = new Mercury(new S3("test-bucket"), mock[WebService with Authorization])
  }

  "Mercury" should {
    "pull in one file from S3" in new Context {
      val file = new File(s"$s3Directory/test-file.txt")

      val pulledFiles = mercury.s3.push(file.getName, file) flatMap { _ =>
        mercury pull Seq(Attachment(file.getName, file.getName, `text/plain`))
      }

      pulledFiles must beLike[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] {
        case Seq(f) => f.key mustEqual file.getName
      }.await
    }

    "pull in two files from S3" in new Context {
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