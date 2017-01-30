package uk.gov.homeoffice.mercury

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorRef, Props}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Message, SQS, SQSActor}
import uk.gov.homeoffice.web.WebService

object MercuryActor {
  def props(sqs: SQS, s3: S3, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) = Props {
    // TODO Do we need a CryptoFilter (if we do, then we need these "secrets")?
    // implicit val secrets = Secrets(config.getString("amazon.sqs.encryption-key"), config.getString("amazon.sqs.signing-password"))
    new MercuryActor(sqs, s3, webService/*, new CryptoFilter*/)
  }
}

class MercuryActor(sqs: SQS, val s3: S3, val webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SQSActor(sqs) with Mercury {
  override def receive: Receive = {
    case m: Message =>
      val client = sender()

      publish(m).map { caseRef => // TODO Is it just "caseRef"???
        client ! caseRef
      }
  }
}