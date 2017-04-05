package uk.gov.homeoffice.mercury

import java.io.File

import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import uk.gov.homeoffice.json.JsonFormats
import uk.gov.homeoffice.web.WebServiceSpecification

import scala.concurrent.duration._
import scala.language.postfixOps

class MercuryPublicationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with JsonFormats with Mockito with NoLanguageFeatures {
  "Mercury publication" should {
    "publish a resource converted to pdf" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-email.txt")
      val fileName = file.getName
      val key = fileName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type application/pdf
          val Seq(FilePart("file", _, Some("application/pdf"), _)) = request.body.files
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

    "publish a resource converted to pdf within a folder structure" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-email.txt")
      val fileName = file.getName
      val key = s"folder/$fileName"

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type application/pdf
          val Seq(FilePart("file", _, Some("application/pdf"), _)) = request.body.files
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

    "fail to publish a resource because of not being authorized" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-email.txt")
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
      val file = new File(s"$s3Directory/test-email.txt")
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
  }
}