package uk.gov.homeoffice.mercury

import java.io.ByteArrayInputStream
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.IOResult
import akka.stream.scaladsl.StreamConverters.fromInputStream
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import play.api.http.Status._
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.homeoffice.aws.s3.{Attachment, S3}
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.WebService

trait Mercury {
  val s3: S3

  val webService: WebService

  val pull: Seq[Attachment] => Future[Iterable[FilePart[Source[ByteString, Future[IOResult]]]]] = { attachments =>
    val fileParts = attachments map { attachment =>
      s3 pull attachment.key map { pull =>
        val data = StreamConverters.fromInputStream(() => pull.inputStream)
        FilePart(attachment.key, attachment.fileName, Some(attachment.contentType), data) // TODO Key? Name of file? The type?
      }
    }

    Future.sequence(fileParts)
  }

  // TODO - Future[String]? What do we really want to get back? Case ref, email address ...
  val publish: Message => Future[String] = { m =>
    val attachments: Message => Future[Seq[Attachment]] = { message =>
      val attachments = message.sqsMessage.getAttributes.toMap.map { case (k, v) =>
        // Either there is 1 attachment, which has not been identified with an index (so add an index) or there is 1 or more attachments with an index
        if (k contains ".") k -> v
        else s"$k.1" -> v
      }.groupBy { case (k, _) =>
        // Group message attributes by indexes since there can be 0, 1 or more attachments
        k.dropWhile(_ != '.')
      }.map { case (key, messageAttributes) =>
        // Attachments won't actually be held in S3 with indexes, so we have to strip them out
        key -> messageAttributes.map { case (k, v) =>
          k.takeWhile(_ != '.') -> v
        }
      }.toList
       .sortBy(_._1) // Sort by keys i.e. S3 keys
       .map { case (_, messageAttributes) =>
        // Generate the Attachments representing what will be in S3
        (messageAttributes.get("key"), messageAttributes.get("fileName"), messageAttributes.get("contentType")) match {
          case (Some(key), Some(fileName), Some(contentType)) =>
            MediaType.parse(contentType) match {
              case Right(mediaType) =>
                Future successful Attachment(key, fileName, mediaType)

              case Left(listOfErrorInfo) =>
                Future failed new Exception(s"""Invalid attachment with key = "$key", file name = "$fileName" because of given invalid content type = "$contentType": ${listOfErrorInfo.mkString}""")
            }

          case (key, fileName, contentType) =>
            val k = key.fold("key = <missing>") { k => s"""key = "$k"""" }
            val f = fileName.fold("file name = <missing>") { f => s"""file name = "$f"""" }
            val c = contentType.fold("content type = <missing>") { c => s"""content type = "$c"""" }

            Future failed new Exception(s"Invalid attachment for $k, $f, $c")
        }
      }

      Future.sequence(attachments)
    }

    attachments(m).flatMap { as =>
      pull(as) flatMap { fileParts =>
        val email = fromInputStream(() => new ByteArrayInputStream(m.content.getBytes))
        val emailFilePart = FilePart("email", "email.txt", Some(`text/plain`), email)

        webService endpoint "/alfresco/s/cmis/p/CTS/Cases/children" post Source(List(emailFilePart) ++ fileParts) flatMap { response =>
          response.status match {
            case OK => Future successful "caseRef" // TODO
            case _ => Future failed new Exception(s"""Failed to publish email to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
          }
        }
      }
    }
  }
}