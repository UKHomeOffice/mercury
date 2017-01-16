package uk.gov.homeoffice.mercury.aws

import java.util.UUID
import akka.actor.Props
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.sqs.subscription.Subscriber
import uk.gov.homeoffice.aws.sqs.{EmbeddedSQSServer, Queue}

class MercurySubscriberActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification {
  trait Context extends ActorSystemContext with ActorExpectations with EmbeddedSQSServer {
    val queueName = UUID.randomUUID().toString
    val queue = create(new Queue(queueName))
  }

  "AWS SQS message" should {
    "be captured as plain text via subscription" in new Context {
      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercurySubscriberActor(new Subscriber(queue)))
      subscriberActor ! createMessage(message)

      eventuallyExpectMsg[String] {
        case response => response == s"Received: $message"
      }
    }
  }
}