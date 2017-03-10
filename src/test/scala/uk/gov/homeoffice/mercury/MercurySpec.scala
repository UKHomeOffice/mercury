package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercurySpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito with NoLanguageFeatures {
  "Mercury authorization" should {
    "fail because of missing user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("", password) must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of invalid user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("wrong", password) must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of missing password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "") must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of invalid password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "wrong") must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "pass resulting in a given token" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, password) must beLike[WebService] {
          case webService: WebService with Authorization => webService.token mustEqual ticket
        }.awaitFor(30 seconds)
      }
    }
  }

  "Mercury event" should {
    "be parsed into a resource key" in new MercuryServicesContext {
      val mercury = new Mercury(mock[S3], mock[WebService with Authorization])

      mercury parse mercuryEventMessage("my-key") mustEqual "my-key"
    }
  }

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
  }

  "Mercury complete event" should {
    "parsed and processed" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName
      val key = fileName

      val message = s"""
      {
        "Records": [{
          "eventVersion": "2.0",
          "eventSource": "aws:s3",
          "awsRegion": "eu-west-2",
          "eventTime": "2017-03-04T23:08:46.241Z",
          "eventName": "ObjectCreated:Put",
          "userIdentity": {
            "principalId": "blah"
          },
          "requestParameters": {
            "sourceIPAddress": "1.1.1.1"
          },
          "responseElements": {
            "x-amz-request-id": "blah",
            "x-amz-id-2": "blah"
          },
          "s3": {
            "s3SchemaVersion": "1.0",
            "configurationId": "resource-uploaded",
            "bucket": {
              "name": "my-bucket",
              "ownerIdentity": {
                "principalId": "blah"
              },
              "arn": "arn:aws:s3:::my-bucket"
            },
            "object": {
              "key": "$key",
              "size": 9,
              "eTag": "blah",
              "sequencer": "blah"
            }
          }
        }]
      }"""

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
          publication <- mercury publish createMessage(message)
        } yield publication

        publication must beAnInstanceOf[Publication].awaitFor(30 seconds)
      }
    }
  }
}