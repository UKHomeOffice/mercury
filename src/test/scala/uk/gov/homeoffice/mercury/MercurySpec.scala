package uk.gov.homeoffice.mercury

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercurySpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  "Mercury authorization" should {
    "fail because of missing user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("", password) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail because of invalid user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("wrong", password) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail because of missing password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail because of invalid password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "wrong") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "pass resulting in a given token" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, password) must beLike[WebService] {
          case webService: WebService with Authorization => webService.token mustEqual ticket
        }.await
      }
    }
  }

  "Mercury publication" should {
    "publish a resource" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", `fileName`, Some("text/plain; charset=UTF-8"), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val publication = for {
          webService <- Mercury authorize credentials
          mercury = Mercury(s3, webService)
          _ <- s3.push(s"folder/$fileName", file)
          publication <- mercury publish createMessage("folder")
        } yield publication

        publication must beEqualTo(Publication("caseRef")).awaitFor(30.seconds)
      }
    }

    "publish two resources" in new MercuryServicesContext {
      val file1 = new File(s"$s3Directory/test-file.txt")
      val fileName1 = file1.getName

      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val fileName2 = file2.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", `fileName1`, Some("text/plain; charset=UTF-8"), _),
                  FilePart("file", `fileName2`, Some("text/plain; charset=UTF-8"), _)) = request.body.files
          Ok
        }
      }) { implicit ws =>
        val publication = for {
          webService <- Mercury authorize credentials
          mercury = Mercury(s3, webService)
          _ <- s3.push(s"folder/$fileName1", file1)
          _ = TimeUnit.SECONDS.sleep(1) // Let's make sure we have an ordering of resources we can assert against
          _ <- s3.push(s"folder/$fileName2", file2)
          publication <- mercury publish createMessage("folder")
        } yield publication

        publication must beEqualTo(Publication("caseRef")).awaitFor(30.seconds)
      }
    }

    "publish resource converted to PDF" in new MercuryServicesContext {
      todo
    }

    "fail to publish a resource because of not being authorized" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName

      routes(authorizeRoute orElse authorizeCheck) { implicit ws =>
        val publication = for {
          webService <- Mercury authorize credentials
          mercury = new Mercury(s3, webService) {
            override lazy val authorizationParam = "" -> ""
          }
          _ <- s3.push(s"folder/$fileName", file)
          publication <- mercury publish createMessage("folder")
        } yield publication

        publication must throwAn[Exception](message = "401, Unauthorized").awaitFor(30.seconds)
      }
    }

    "fail to publish a resource because endpoint is not available" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case _ => Action(BadGateway)
      }) { implicit ws =>
        val publication = for {
          webService <- Mercury authorize credentials
          mercury = Mercury(s3, webService)
          _ <- s3.push(s"folder/$fileName", file)
          publication <- mercury publish createMessage("folder")
        } yield publication

        publication must throwAn[Exception](message = "502, Bad Gateway").awaitFor(30.seconds)
      }
    }

    "fail to publish because there are no resources" in new MercuryServicesContext {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case _ => Action(Ok)
      }) { implicit ws =>
        val publication = for {
          webService <- Mercury authorize credentials
          mercury = Mercury(s3, webService)
          publication <- mercury publish createMessage("folder")
        } yield publication

        publication must throwAn[Exception](message = """No existing resources on S3 for given SQS event "folder"""").awaitFor(30.seconds)
      }
    }
  }
}