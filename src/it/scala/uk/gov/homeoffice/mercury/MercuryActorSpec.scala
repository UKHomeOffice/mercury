package uk.gov.homeoffice.mercury

import java.io.File
import scala.util.Try
import akka.actor.Props
import akka.testkit.TestActorRef
import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{SQS, _}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.boot.configuration.{HocsCredentials, HocsWebService}
import uk.gov.homeoffice.web.WebService

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * export WEB_SERVICES_HOCS_URI="<host>"; export WEB_SERVICES_HOCS_LOGIN_USER_NAME="<userName>"; export WEB_SERVICES_HOCS_LOGIN_PASSWORD="<password>"; sbt it:test-only *MercuryActorSpec
  * OR
  * sbt -DWEB_SERVICES_HOCS_URI=<host> -DWEB_SERVICES_HOCS_LOGIN_USER_NAME=<userName> -DWEB_SERVICES_HOCS_LOGIN_PASSWORD=<password> "it:test-only *MercuryActorSpec"
  * </pre>
  * If none of the above environment variables are provided, then everything defaults to localhost services which can be achieved by first starting up "docker-compose up" before "it:test-only *MercuryActorSpec"
  * @param env ExecutionEnv For asynchronous testing
  */
class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with HasConfig with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations {
    var sqs: SQS = _
    var s3: S3 = _
    var hocsWebService: WebService = _

    override def around[R: AsResult](r: => R): Result = try {
      sqs = uk.gov.homeoffice.mercury.boot.configuration.SQS()
      s3 = uk.gov.homeoffice.mercury.boot.configuration.S3(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))
      hocsWebService = HocsWebService()

      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      Try { sqs.sqsClient.shutdown() }
      Try { s3.s3Client.shutdown() }
      Try { hocsWebService.wsClient.close() }
    }
  }

  "Mercury" should {
    "consume SQS message, acquire its associated file and stream these to HOCS" in new Context {
      val mercuryActor = TestActorRef {
        Props {
          new MercuryActor(sqs, s3, HocsCredentials(), hocsWebService)(Seq(testActor))
        }
      }

      val file = new File("src/test/resources/s3/test-file.txt")

      s3.push(s"folder/${file.getName}", file).map { _ =>
        implicit val sqsClient = sqs.sqsClient

        sqsClient.sendMessage(queueUrl(sqs.queue.queueName), "folder")
      }

      eventuallyExpectMsg[String] {
        case s => s == "caseRef"
      }
    }
  }
}