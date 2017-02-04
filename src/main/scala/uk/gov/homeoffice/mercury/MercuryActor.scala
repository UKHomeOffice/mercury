package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
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
  override def preStart(): Unit = {
    super.preStart()

    Mercury authorize login map { webService =>
      context become authorized(webService)
    }
  }

  override def receive: Receive = {
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

        mercury.publish(m).map { caseRef => // TODO Is it just "caseRef"???
          client ! caseRef
        }
    }
  }
}