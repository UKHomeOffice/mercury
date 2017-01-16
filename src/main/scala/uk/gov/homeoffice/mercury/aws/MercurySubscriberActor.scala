package uk.gov.homeoffice.mercury.aws

import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.aws.sqs.subscription.{Subscriber, SubscriberActor}

class MercurySubscriberActor(subscriber: Subscriber) extends SubscriberActor(subscriber) {
  override def receive: Receive = {
    case m: Message => sender ! s"Received: ${m.content}"
  }
}