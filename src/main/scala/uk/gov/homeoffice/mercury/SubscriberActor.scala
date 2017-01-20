package uk.gov.homeoffice.mercury

import akka.actor.ActorRef
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.aws.sqs.subscribe.Subscriber

class SubscriberActor(subscriber: Subscriber)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends uk.gov.homeoffice.aws.sqs.subscribe.SubscriberActor(subscriber) {
  override def receive: Receive = {
    case m: Message => sender() ! s"Handled message: ${m.content}"
  }
}