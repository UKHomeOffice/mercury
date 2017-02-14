package uk.gov.homeoffice.mercury

import java.util.concurrent.TimeUnit
import scala.util.{Success, Try}
import akka.testkit.TestActorRef
import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.boot.configuration.{HocsCredentials, HocsWebService, S3, SQS}

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with HasConfig with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations {
    /*implicit val listeners = Seq(testActor)

    val sqs = Try {
      SQS()
    }

    val s3 = Try {
      S3(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))
    }

    val hocsWebService = Try {
      HocsWebService()
    }

    val mercuryActor = (sqs, s3, hocsWebService) match {
      case (Success(s), Success(ss), Success(h)) =>
        system.actorOf(MercuryActor.props(s, ss, HocsCredentials(), h), name = "mercury-it-actor")

      case _ =>
        TestActorRef("blah")
    }


    override def around[R: AsResult](r: => R): Result = try {
      super.around(r)
    } finally {
      sqs.map(_.sqsClient.shutdown())
      s3.map(_.s3Client.shutdown())
      hocsWebService.map(_.wsClient.close())
    }*/
  }

  "Mercury" should {
    "consume SQS message, acquire its associated file and stream these to HOCS" in new Context {
      pending

      // s3 push TODO
      //val sendMessageRequest = new SendMessageRequest(sqs.queue.queueName, "Test Message")

      //sqs.sqsClient.sendMessage(sendMessageRequest)

      /*eventuallyExpectMsg[String] {
        case msg: String => msg == "caseRef" // TODO
      }*/

      TimeUnit.SECONDS.sleep(5)

      ok
    }
  }
}