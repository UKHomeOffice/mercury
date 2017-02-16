package uk.gov.homeoffice.mercury

import akka.actor.Props
import akka.testkit.TestActorRef
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Queue, SQS}
import uk.gov.homeoffice.web.WebServiceSpecification

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with MercuryServicesContext {
    implicit val listeners = Seq(testActor)

    val queue = create(new Queue("test-queue"))
  }

  "Mercury actor" should {
    "receive an AWS SQS message as plain text, but not be in an authorized state to publish it" in new Context {
      routes(PartialFunction.empty) { webService =>
        val message = "A plain text message"

        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), mock[S3], credentials, webService) {
              override def preStart(): Unit = () // Avoid attempt to automatically authorize
            }
          }
        }

        mercuryActor ! createMessage(message)

        eventuallyExpectMsg[String] {
          case response => response == "Received a message but not authorized to publish it"
        }
      }
    }

    "receive an AWS SQS message as plain text" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action {
          Ok
        }
      }) { webService =>
        val message = "A plain text message"

        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), mock[S3], credentials, webService)
          }
        }

        eventuallyExpectMsg[Authorized.type]()

        mercuryActor ! createMessage(message)

        eventuallyExpectMsg[String] {
          case response => response == "caseRef"
        }
      }
    }
  }

  "Mercury actor subscription" should {
    "be captured as plain text" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action {
          Ok
        }
      }) { webService =>
        implicit val listeners = Seq(testActor)

        val message = "A plain text message"

        val sqs = new SQS(queue)

        TestActorRef {
          Props {
            new MercuryActor(sqs, mock[S3], credentials, webService)
          }
        }

        sqs publish message

        eventuallyExpectMsg[String] {
          case response => response == "caseRef"
        }
      }
    }
  }
}