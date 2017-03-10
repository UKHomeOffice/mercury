package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.json.JsonFormats
import uk.gov.homeoffice.web.WebServiceSpecification

class MercuryPublicationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with JsonFormats with Mockito with NoLanguageFeatures {
  "Mercury publication" should {
    "publish a resource" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName
      val key = fileName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", `fileName`, Some("text/plain; charset=UTF-8"), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val publication = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized)
          _ <- s3.push(key, file)
          publication <- mercury publish mercuryEventMessage(key)
        } yield publication

        publication must beAnInstanceOf[Publication].awaitFor(30 seconds)
      }
    }

    "publish a resource within a folder stucture" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName
      val key = s"folder/$fileName"

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", `fileName`, Some("text/plain; charset=UTF-8"), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val publication = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized)
          _ <- s3.push(key, file)
          publication <- mercury publish mercuryEventMessage(key)
        } yield publication

        publication must beAnInstanceOf[Publication].awaitFor(30 seconds)
      }
    }

    "publish resource converted to PDF" in new MercuryServicesContext {
      todo
    }

    "fail to publish a resource because of not being authorized" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName
      val key = fileName

      routes(authorizeRoute orElse authorizeCheck) { implicit ws =>
        val publication = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized) {
            override lazy val authorizationParam = "" -> ""
          }
          _ <- s3.push(key, file)
          publication <- mercury publish mercuryEventMessage(key)
        } yield publication

        publication must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail to publish a resource because endpoint is not available" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName
      val key = fileName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case _ => Action(BadGateway)
      }) { implicit ws =>
        val publication = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized)
          _ <- s3.push(key, file)
          publication <- mercury publish mercuryEventMessage(key)
        } yield publication

        publication must throwAn[Exception](message = "502, Bad Gateway").awaitFor(30 seconds)
      }
    }

    "fail to publish resource because it does not exist" in new MercuryServicesContext {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case _ => Action(Ok)
      }) { implicit ws =>
        val publication = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized)
          publication <- mercury publish mercuryEventMessage("non-existing-resource")
        } yield publication

        publication must throwAn[Exception](message = "The resource you requested does not exist").awaitFor(30 seconds)
      }
    }

    "publish resources" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file
          val Seq(FilePart("file", fileName, _, _)) = request.body.files
          fileName must endWith(file.getName)
          Ok(Json.obj("file" -> fileName))
        }
      }) { implicit ws =>
        val publications = for {
          webServiceAuthorized <- Mercury authorize credentials
          mercury = new Mercury(s3, webServiceAuthorized)
          _ <- s3.push(s"folder/${file.getName}", file)
          publications <- mercury publish
        } yield publications filter { p =>
          // Filtering gimmick because Fake S3 adds extra file with prefix (in this case "folder")
          (p.result \ "file").extract[String] == file.getName
        }

        publications must beLike[Seq[Publication]] {
          case ps => ps.size mustEqual 1
        }.awaitFor(30.seconds)
      }
    }
  }
}