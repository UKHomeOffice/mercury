package uk.gov.homeoffice.mercury.s3

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Actor, ActorRef, Props}
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.mercury.Protocol.{AuthorizeMercury, Authorized, Publish}
import uk.gov.homeoffice.mercury.{Authorization, Credentials, Mercury}
import uk.gov.homeoffice.web.WebService

/**
  * Companion object to create Mercury Actor that only interfaces, via Mercury, with AWS S3
  */
object MercuryActor {
  def props(s3: S3, credentials: Credentials, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) = Props {
    new MercuryActor(s3, credentials, webService)
  }
}

/**
  * Actor to consume/poll for resources on AWS S3 and publish them to a web service endpoint exposed by HOCS.
  * This Actor can be in one of two states:
  * - Not (HOCS) authorized is the initial/default state
  * - Authorized to publish resources to HOCS
  * @param s3 S3 To pull in resources from
  * @param credentials Credentials To be validated against AWS
  * @param webService WebService Acting as the interface to endpoints exposed by HOCS
  * @param listeners Seq[ActorRef] Any (registered) listeners wishing to be notified of certain events
  */
class MercuryActor(val s3: S3, credentials: Credentials, implicit val webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends Actor with Logging {
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
      info(s"Authorizing Mercury...")

      Mercury authorize credentials map { webService =>
        context become authorized(webService)
        context.system.scheduler.scheduleOnce(10 seconds, self, Publish)
      } recover {
        case t: Throwable =>
          error(s"Failed to authorize Mercury with webservice ${webService.host} because of ${t.getMessage}")
          context.system.scheduler.scheduleOnce(10 seconds, self, AuthorizeMercury)
      }

    case Publish =>
      val warning = "Mercury was triggered but is not authorized to perform publication"
      warn(warning)
      sender() ! warning
  }

  /**
    * Authorized state where resources can be published to HOCS
    * @param webService WebService with Authorization
    * @return Receive
    */
  def authorized(webService: WebService with Authorization): Receive = {
    info("Mercury has been authorized")
    listeners foreach { _ ! Authorized }

    val mercury = Mercury(s3, webService)

    val receive: Receive = {
      case Publish =>
        val client = sender()

        mercury.publish.map { publications =>
          client ! publications
          listeners foreach { _ ! publications }
          context.system.scheduler.scheduleOnce(10 seconds, self, Publish)
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