package uk.gov.homeoffice.mercury

import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{ActorRef, Props}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Message, SQS, SQSActor}
import uk.gov.homeoffice.web.WebService

object MercuryActor {
  def props(sqs: SQS, s3: S3, login: Login, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) = Props {
    // TODO Do we need a CryptoFilter (if we do, then we need these "secrets")?
    // implicit val secrets = Secrets(config.getString("amazon.sqs.encryption-key"), config.getString("amazon.sqs.signing-password"))
    new MercuryActor(sqs, s3, login, webService/*, new CryptoFilter*/)
  }
}

class MercuryActor(sqs: SQS, val s3: S3, login: Login, implicit val webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SQSActor(sqs) {
  implicit val ec = context.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    self ! AuthorizeMercury
  }

  override def receive: Receive = {
    case AuthorizeMercury =>
      Mercury authorize login map { webService =>
        context become authorized(webService)
      } recover {
        case t: Throwable =>
          error(s"Failed to authorize Mercury with webservice ${webService.host} because of ${t.getMessage}")
          context.system.scheduler.scheduleOnce(10 seconds, self, AuthorizeMercury)
      }

    case m: Message =>
      val warning = "Received a message but not authorized to publish it"
      warn(warning)
      sender() ! warning
  }

  def authorized(webService: WebService with Authorization): Receive = {
    val mercury = Mercury(s3, webService)

    {
      case m: Message =>
        val client = sender()

        mercury publish m map { caseRef => // TODO Is it just "caseRef"???
          client ! caseRef
        }
    }
  }
}

case object AuthorizeMercury