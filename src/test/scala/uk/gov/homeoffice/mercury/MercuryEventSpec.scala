package uk.gov.homeoffice.mercury

import java.io.File

import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

import scala.concurrent.duration._
import scala.language.postfixOps

class MercuryEventSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito with NoLanguageFeatures {
  "Mercury event" should {
    "be parsed into a resource key" in new MercuryServicesContext {
      val mercury = new Mercury(mock[S3], mock[WebService with Authorization])

      mercury parse mercuryEventMessage("my-key") mustEqual "my-key"
    }
  }

  "Mercury complete event" should {
    "parsed and processed" in new MercuryServicesContext {
      val file = new File(s"$s3Directory/test-email.txt")
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
        case POST(p"/alfresco/s/homeoffice/ctsv2/createCase") => Action(parse.multipartFormData) { request =>
          // Expect one file of type application/pdf
          val Seq(FilePart("file", _, Some("application/pdf"), _)) = request.body.files
          Ok(Json.obj("caseRef" -> "CaseRef"))
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