package uk.gov.homeoffice.mercury

import akka.actor.Props
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Queue, SQS, SQSServerEmbedded}
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with SQSServerEmbedded {
    val queue = create(new Queue("test-queue"))
  }

  "Mercury actor" should {
    "receive an AWS SQS message as plain text" in new Context {
      val message = "A plain text message"

      val mercuryActor = system actorOf Props(new MercuryActor(new SQS(queue), mock[S3], mock[WebService]))
      mercuryActor ! createMessage(message)

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }

  "Mercury actor subscription" should {
    "be captured as plain text" in new Context {
      implicit val listeners = Seq(testActor)

      val message = "A plain text message"

      val sqs = new SQS(queue)
      system actorOf Props(new MercuryActor(sqs, mock[S3], mock[WebService]))

      sqs publish message

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }
}