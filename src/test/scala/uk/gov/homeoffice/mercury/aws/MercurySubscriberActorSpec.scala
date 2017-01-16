package uk.gov.homeoffice.mercury.aws

import akka.actor.Props
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.sqs.subscription.Subscriber
import uk.gov.homeoffice.aws.sqs.{EmbeddedSQSServer, Publisher, Queue}

class MercurySubscriberActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification {
  trait Context extends ActorSystemContext with ActorExpectations with EmbeddedSQSServer {
    val queue = create(new Queue("test-queue"))
  }

  "AWS SQS message" should {
    "be captured directly as plain text" in new Context {
      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercurySubscriberActor(new Subscriber(queue)))
      subscriberActor ! createMessage(message)

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }

    "be captured as plain text via subscription" in new Context {
      implicit val listeners = Seq(testActor)

      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercurySubscriberActor(new Subscriber(queue)))

      val publisher = new Publisher(queue)
      publisher publish message

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }
}