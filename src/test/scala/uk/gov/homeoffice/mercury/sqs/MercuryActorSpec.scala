package uk.gov.homeoffice.mercury.sqs

import java.io.File

import akka.actor.Props
import akka.testkit.TestActorRef
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.sqs.{Queue, SQS}
import uk.gov.homeoffice.mercury.Protocol.Authorized
import uk.gov.homeoffice.mercury.{MercuryServicesContext, Publication}
import uk.gov.homeoffice.web.WebServiceSpecification

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with MercuryServicesContext {
    implicit val listeners = Seq(testActor)

    val queue = create(new Queue("test-queue"))
  }

  "Mercury actor" should {
    "receive a Mercury SQS message event, but not be in an authorized state to publish it" in new Context {
      routes(PartialFunction.empty) { webService =>
        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), s3, credentials, webService) {
              override def preStart(): Unit = () // Avoid attempt to automatically authorize
            }
          }
        }

        mercuryActor ! mercuryEventMessage("resource-pending")

        eventuallyExpectMsg[String] {
          case response => response == "Received a message but Mercury is not authorized to perform publication"
        }
      }
    }

    "receive a Mercury SQS message event but not have anything to publish" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/ctsv2/createCase") => Action {
          Ok(Json.obj("caseRef" -> "CaseRef"))
        }
      }) { webService =>
        val mercuryActor = TestActorRef(Props(new MercuryActor(new SQS(queue), s3, credentials, webService)))

        eventuallyExpectMsg[Authorized.type]()

        mercuryActor ! mercuryEventMessage("no-resource")

        eventuallyExpectMsg[Throwable] {
          case throwable: Throwable => throwable.getMessage.startsWith("The resource you requested does not exist")
        }
      }
    }

    "receive a Mercury SQS message event and be notified when the associated resource has been published" in new Context {
      val file = new File(s"$s3Directory/test-email.txt")
      val fileName = file.getName
      val key = fileName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/ctsv2/createCase") => Action(parse.multipartFormData) { request =>
          // Expect one file of type application/pdf
          val Seq(FilePart("file", _, Some("application/pdf"), _)) = request.body.files
          Ok(Json.obj("caseRef" -> "CaseRef"))
        }
      }) { webService =>
        val mercuryActor = TestActorRef(Props(new MercuryActor(new SQS(queue), s3, credentials, webService)))

        eventuallyExpectMsg[Authorized.type]()

        s3.push(key, file).foreach { _ =>
          mercuryActor ! mercuryEventMessage(key)
        }

        eventuallyExpectMsg[Publication]()
      }
    }

    "restart (and so re-authorize) upon receiving an exception" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/ctsv2/createCase") => Action(parse.multipartFormData) { _ =>
          BadRequest
        }
      }) { webService =>
        val mercuryActor = TestActorRef(Props(new MercuryActor(new SQS(queue), s3, credentials, webService)))

        eventuallyExpectMsg[Authorized.type]()

        mercuryActor ! mercuryEventMessage("")

        eventuallyExpectMsg[Authorized.type]()
      }
    }
  }
}