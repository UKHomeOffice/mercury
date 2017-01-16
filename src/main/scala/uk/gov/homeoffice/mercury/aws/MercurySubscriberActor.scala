package uk.gov.homeoffice.mercury.aws

import akka.actor.ActorRef
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.aws.sqs.subscription.{Subscriber, SubscriberActor}

class MercurySubscriberActor(subscriber: Subscriber)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SubscriberActor(subscriber) {
  override def receive: Receive = {
    case m: Message => sender() ! s"Handled message: ${m.content}"
  }
}