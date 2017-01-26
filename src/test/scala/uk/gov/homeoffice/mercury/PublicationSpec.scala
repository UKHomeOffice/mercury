package uk.gov.homeoffice.mercury

import java.io.{ByteArrayInputStream, File, FileInputStream}
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.scaladsl.StreamConverters._
import akka.stream.scaladsl.{Source, StreamConverters}
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.mvc.{Action, Handler, RequestHeader}
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.WebServiceSpecification

class PublicationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification {
  val emailsEndpoint = "/alfresco/s/cmis/p/CTS/Cases/children"

  val emailsRoute: PartialFunction[RequestHeader, Handler] = {
    case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
      println(s"===> ${request.rawQueryString}")

      request.body.files.foreach { filePart =>
        println(s"===> ${filePart.filename}")
        filePart.ref.moveTo(new File(s"./read-${filePart.filename}"))
      }

      Ok
    }
  }

  "Publisher" should {
    "publish email as plain text without any attachments" in routes(emailsRoute) { webService =>
      val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes))

      val emailFilePart = FilePart("email", "email.txt", Some(`text/plain`), email)

      webService endpoint emailsEndpoint post Source(List(emailFilePart)) must beLike[WSResponse] {
        case r: WSResponse => r.status mustEqual OK
      }.await
    }

    "publish email translated to PDF without any attachments" in routes(emailsRoute) { webService =>
      val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes)) // TODO translation
      val emailFilePart = FilePart("email", "email.pdf", Some(`application/pdf`), email)

      webService endpoint emailsEndpoint post Source(List(emailFilePart)) must beLike[WSResponse] {
        case r: WSResponse => r.status mustEqual OK
      }.await
    }

    "publish email with an attachment" in routes(emailsRoute) { webService =>
      val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes))
      val emailFilePart = FilePart("email", "email.pdf", Some(`text/plain`), email)

      val attachment = StreamConverters.fromInputStream(() => new FileInputStream("./src/test/resources/s3/test-file.txt"))
      val attachmentFilePart = FilePart("test-key", "test-file.txt", Some(`text/plain`), attachment) // TODO Work out type of attachment

      webService endpoint emailsEndpoint post Source(List(emailFilePart, attachmentFilePart)) must beLike[WSResponse] {
        case r: WSResponse => r.status mustEqual OK
      }.await
    }

    "publish email with two attachments" in routes(emailsRoute) { webService =>
      val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes))
      val emailFilePart = FilePart("email", "email.pdf", Some(`text/plain`), email)

      val attachment1 = StreamConverters.fromInputStream(() => new FileInputStream("./src/test/resources/s3/test-file.txt"))
      val attachment1FilePart = FilePart("test-key", "test-file.txt", Some(`text/plain`), attachment1)

      val attachment2 = StreamConverters.fromInputStream(() => new FileInputStream("./src/test/resources/s3/test-file-2.txt"))
      val attachment2FilePart = FilePart("test-key-2", "test-file-2.txt", Some(`text/plain`), attachment2)

      webService endpoint emailsEndpoint post Source(List(emailFilePart, attachment1FilePart, attachment2FilePart)) must beLike[WSResponse] {
        case r: WSResponse => r.status mustEqual OK
      }.await
    }
  }
}