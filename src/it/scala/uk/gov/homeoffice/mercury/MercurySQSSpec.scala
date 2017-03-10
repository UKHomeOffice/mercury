package uk.gov.homeoffice.mercury

import scala.util.Try
import com.amazonaws.services.sqs.model.{ReceiveMessageRequest, SendMessageRequest}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.sqs.{SQS, _}
import uk.gov.homeoffice.specs2.ComposableAround

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * sbt '; set javaOptions ++= Seq("-DAWS_SQS_URI=<host>", "-DAWS_SQS_CREDENTIALS_ACCESS_KEY=<access-key>", "-DAWS_SQS_CREDENTIALS_SECRET_KEY=<secret-key>"); it:test-only *MercurySQSSpec'
  * </pre>
  * If none of the above environment variables are provided, then everything defaults to localhost services which can be achieved by first starting up "docker-compose up" before "it:test-only *MercurySQSSpec"
  * @param env ExecutionEnv For asynchronous testing
  */
class MercurySQSSpec(implicit env: ExecutionEnv) extends Specification {
  trait Context extends ComposableAround {
    var sqs: SQS = _

    override def around[R: AsResult](r: => R): Result = try {
      sqs = uk.gov.homeoffice.mercury.boot.configuration.SQS()

      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      Try { sqs.sqsClient.shutdown() }
    }
  }

  "Mercury" should {
    "consume SQS message, acquire its associated file and stream these to HOCS" in new Context {
      implicit val sqsClient = sqs.sqsClient

      val sendMessageRequest = new SendMessageRequest(queueUrl(sqs.queue.queueName), "Ye Baby")
      sqs.sqsClient.sendMessage(sendMessageRequest)
      sqs.sqsClient.sendMessage(sendMessageRequest.withMessageBody("Ye Baby 2"))

      val receiveMessageResult = sqs.sqsClient.receiveMessage(new ReceiveMessageRequest(queueUrl(sqs.queue.queueName)).withMaxNumberOfMessages(10))
      receiveMessageResult.getMessages.get(0).getBody mustEqual "Ye Baby 2"
    }
  }
}