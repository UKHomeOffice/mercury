package uk.gov.homeoffice.mercury.s3

import java.io.File
import akka.actor.Props
import akka.testkit.TestActorRef
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.mercury.{MercuryServicesContext, Publication}
import uk.gov.homeoffice.mercury.Protocol.{Authorized, Publish}
import uk.gov.homeoffice.web.WebServiceSpecification

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with MercuryServicesContext {
    implicit val listeners = Seq(testActor)
  }

  "Mercury actor" should {
    "be triggered to publish, but not be in an authorized state to publish" in new Context {
      routes(PartialFunction.empty) { webService =>
        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(s3, credentials, webService) {
              override def preStart(): Unit = () // Avoid attempt to automatically authorize
            }
          }
        }

        mercuryActor ! Publish

        eventuallyExpectMsg[String] {
          case response => response == "Mercury was triggered but is not authorized to perform publication"
        }
      }
    }

    "be triggered to publish, but not have anything to publish" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action {
          Ok
        }
      }) { webService =>
        TestActorRef(Props(new MercuryActor(s3, credentials, webService)))

        eventuallyExpectMsg[Authorized.type]()

        eventuallyExpectMsg[Seq[Publication]] {
          case publications => publications == Nil
        }
      }
    }

    "be triggered to publish and be notified when the associated resource has been published" in new Context {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file
          val Seq(FilePart("file", fileName, _, _)) = request.body.files
          fileName must endWith(file.getName)
          Ok(Json.obj("file" -> fileName))
        }
      }) { webService =>
        TestActorRef(Props(new MercuryActor(s3, credentials, webService)))

        eventuallyExpectMsg[Authorized.type]()

        s3.push(s"folder/$fileName", file)

        eventuallyExpectMsg[Seq[Publication]]()
      }
    }
  }
}