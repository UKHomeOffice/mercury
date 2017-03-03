package uk.gov.homeoffice.mercury

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorRef, Props}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Message, SQS, SQSActor}
import uk.gov.homeoffice.web.WebService

object MercuryActor {
  def props(sqs: SQS, s3: S3, credentials: Credentials, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) = Props {
    // TODO Do we need a CryptoFilter (if we do, then we need these "secrets")?
    // implicit val secrets = Secrets(config.getString("amazon.sqs.encryption-key"), config.getString("amazon.sqs.signing-password"))
    new MercuryActor(sqs, s3, credentials, webService/*, new CryptoFilter*/)
  }
}

class MercuryActor(sqs: SQS, val s3: S3, credentials: Credentials, implicit val webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SQSActor(sqs) {
  implicit val ec = context.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    self ! AuthorizeMercury
  }

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

  def authorized(webService: WebService with Authorization): Receive = {
    val mercury = Mercury(s3, webService)

    listeners foreach { _ ! Authorized }

    { case message: Message =>
        val client = sender()

        mercury publish message map { publication =>
          client ! publication
          listeners foreach { _ ! publication }
          delete(message)
        } recoverWith {
          case t: Throwable =>
            client ! t
            listeners foreach { _ ! t }
            delete(message)
            Future failed t
        }
    }
  }
}

case object AuthorizeMercury

case object Authorized