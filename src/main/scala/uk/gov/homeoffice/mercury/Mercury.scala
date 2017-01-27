package uk.gov.homeoffice.mercury

import java.io.ByteArrayInputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.IOResult
import akka.stream.scaladsl.StreamConverters.fromInputStream
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import play.api.http.Status._
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.WebService

trait Mercury {
  val s3: S3

  val webService: WebService

  val pull: Seq[String] => Future[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] = { keys =>
    val fileParts = keys map { key =>
      s3 pull key map { pull =>
        val data = StreamConverters.fromInputStream(() => pull.inputStream)
        FilePart(key, key, Some(`text/plain`), data) // TODO Key? Name of file? The type?
      }
    }

    Future.sequence(fileParts)
  }

  // TODO - Future[String]? What do we really want to get back? Case ref, email address ...
  val publish: Message => Future[String] = { m =>
    val keys: Message => Seq[String] = { m =>
      Nil // TODO
    }

    pull(keys(m)) flatMap { attachments =>
      val email = fromInputStream(() => new ByteArrayInputStream(m.content.getBytes))

      val emailFilePart = FilePart("email", "email.txt", Some(`text/plain`), email)

      webService endpoint "/alfresco/s/cmis/p/CTS/Cases/children" post Source(List(emailFilePart) ++ attachments) flatMap { response =>
        response.status match {
          case OK => Future successful "caseRef" // TODO
          case _ => Future failed new Exception(s"""Failed to publish email to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
        }
      }
    }
  }
}