package uk.gov.homeoffice.mercury.sqs

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorRef, Props}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Message, SQS, SQSActor}
import uk.gov.homeoffice.mercury.Protocol.{AuthorizeMercury, Authorized}
import uk.gov.homeoffice.mercury._
import uk.gov.homeoffice.web.WebService

/**
  * Companion object to create Mercury Actor that subscribes to AWS SQS events representing resources to be pulled in from AWS S3
  */
object MercuryActor {
  def props(sqs: SQS, s3: S3, credentials: Credentials, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) = Props {
    new MercuryActor(sqs, s3, credentials, webService)
  }
}

/**
  * Actor to subscribe to AWS SQS events representing resources to be pulled in from AWS S3 and publish them to a web service endpoint exposed by HOCS.
  * This Actor can be in one of two states:
  * - Not (HOCS) authorized is the initial/default state
  * - Authorized to publish resources to HOCS
  * @param sqs SQS To subscribe for events
  * @param s3 S3 To pull in resources from
  * @param credentials Credentials To be validated against AWS
  * @param webService WebService Acting as the interface to endpoints exposed by HOCS
  * @param listeners Seq[ActorRef] Any (registered) listeners wishing to be notified of certain events
  */
class MercuryActor(sqs: SQS, val s3: S3, credentials: Credentials, implicit val webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SQSActor(sqs) {
  implicit val ec = context.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    self ! AuthorizeMercury
  }

  /**
    * Initial/default state of not being (HOCS) authorized
    * @return Receive
    */
  override def receive: Receive = {
    case AuthorizeMercury =>
      Mercury authorize credentials map { webService =>
        context become authorized(webService)
      } recover {
        case t: Throwable =>
          error(s"Failed to authorize Mercury with webservice ${webService.host} because of ${t.getMessage}")
          context.system.scheduler.scheduleOnce(10 seconds, self, AuthorizeMercury)
      }

    case _: Message =>
      val warning = "Received a message but Mercury is not authorized to perform publication"
      warn(warning)
      sender() ! warning
  }

  /**
    * Authorized state where resources can be published to HOCS
    * @param webService WebService with Authorization
    * @return Receive
    */
  def authorized(webService: WebService with Authorization): Receive = {
    listeners foreach { _ ! Authorized }

    val mercury = Mercury(s3, webService)

    val receive: Receive = {
      case message: Message =>
        val client = sender()

        mercury publish message map { publication =>
          delete(message)
          client ! publication
          listeners foreach { _ ! publication }
        } recoverWith {
          case t: Throwable =>
            client ! t
            listeners foreach { _ ! t }
            context.unbecome()
            postRestart(t)
            Future failed t
        }
    }

    receive
  }
}