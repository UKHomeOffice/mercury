package uk.gov.homeoffice.mercury

import java.io.File
import java.util.concurrent.TimeUnit
import scala.util.Try
import akka.actor.ActorRef
import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.sqs.model.{MessageAttributeValue, SendMessageRequest}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.SQS
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.boot.configuration.{HocsCredentials, HocsWebService}
import uk.gov.homeoffice.web.WebService
import uk.gov.homeoffice.aws.sqs._
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * export WEB_SERVICES_HOCS_URI="<host>"; export WEB_SERVICES_HOCS_LOGIN_USER_NAME="<userName>"; export WEB_SERVICES_HOCS_LOGIN_PASSWORD="<password>"; sbt it:test
  * OR
  * sbt -DWEB_SERVICES_HOCS_URI=<host> -DWEB_SERVICES_HOCS_LOGIN_USER_NAME=<userName> -DWEB_SERVICES_HOCS_LOGIN_PASSWORD=<password> "it:test"
  * </pre>
  * @param env ExecutionEnv For asynchronous testing
  */
class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with HasConfig with Mockito {
  val `text/plain`: String = akka.http.scaladsl.model.MediaTypes.`text/plain`

  trait Context extends ActorSystemContext with ActorExpectations {
    implicit val listeners = Seq(testActor)

    var sqs: SQS = _
    var s3: S3 = _
    var hocsWebService: WebService = _

    var mercuryActor: ActorRef = _

    override def around[R: AsResult](r: => R): Result = try {
      sqs = uk.gov.homeoffice.mercury.boot.configuration.SQS()
      s3 = uk.gov.homeoffice.mercury.boot.configuration.S3(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))
      hocsWebService = HocsWebService()

      mercuryActor = system.actorOf(MercuryActor.props(sqs, s3, HocsCredentials(), hocsWebService), name = "mercury-it-actor")

      super.around(r)
    } finally {
      Try { sqs.sqsClient.shutdown() }
      Try { s3.s3Client.shutdown() }
      Try { hocsWebService.wsClient.close() }
    }
  }

  "Mercury" should {
    "consume SQS message, acquire its associated file and stream these to HOCS" in new Context {
      val file1 = new File("src/test/resources/s3/test-file.txt")

      s3.push(file1.getName, file1).map { push =>
        println(s"===> Push: $push")

        implicit val sqsClient = sqs.sqsClient

        val sendMessageRequest =
          new SendMessageRequest(queueUrl(sqs.queue.queueName), "Test Message")
            .addMessageAttributesEntry("key", new MessageAttributeValue().withDataType("String").withStringValue(file1.getName))
            .addMessageAttributesEntry("fileName", new MessageAttributeValue().withDataType("String").withStringValue(file1.getName))
            .addMessageAttributesEntry("contentType", new MessageAttributeValue().withDataType("String").withStringValue(`text/plain`))

        sqs.sqsClient.sendMessage(sendMessageRequest)
      }

      /*s3.pull(file1.getName).map { pull =>
        println("===> Pulled file with: " + scala.io.Source.fromInputStream(pull.inputStream).mkString)
      }*/

      /*eventuallyExpectMsg[String] {
        case msg: String => msg == "caseRef" // TODO
      }*/

      TimeUnit.SECONDS.sleep(5)

      ok
    }
  }
}