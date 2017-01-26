package uk.gov.homeoffice.mercury

import akka.actor.Props
import akka.testkit.TestActorRef
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.sqs.publish.{Publisher => SQSPublisher}
import uk.gov.homeoffice.aws.sqs.subscribe.Subscriber
import uk.gov.homeoffice.aws.sqs.{Queue, SQSServerEmbedded}
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with SQSServerEmbedded {
    val queue = create(new Queue("test-queue"))
  }

  "Mercury" should {
    "receive an AWS SQS message as plain text" in new Context {
      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercuryActor(new Subscriber(queue), mock[WebService]))
      subscriberActor ! createMessage(message)

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }

    "publish a received plain text AWS SQS message" in new Context {
      routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
          Ok
        }
      } { webService =>
        val message = "A plain text message"

        val actor = TestActorRef {
          new MercuryActor(new Subscriber(queue), webService)
        }

        actor.underlyingActor publish createMessage(message) must beEqualTo("caseRef").await
      }
    }
  }

  "AWS SQS messages via subscription" should {
    "be captured as plain text" in new Context {
      implicit val listeners = Seq(testActor)

      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercuryActor(new Subscriber(queue), mock[WebService]))

      val sqsPublisher = new SQSPublisher(queue)
      sqsPublisher publish message

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }
}