package uk.gov.homeoffice.mercury

import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try
import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{Push, S3}
import uk.gov.homeoffice.specs2.ComposableAround

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * export AWS_S3_URI="<host>"; export AWS_S3_CREDENTIALS_ACCESS_KEY="<access-key>"; export AWS_S3_CREDENTIALS_SECRET_KEY="<secret-key>"; sbt it:test-only *MercuryS3Spec
  * OR
  * sbt -DAWS_S3_URI=<host> -DAWS_S3_CREDENTIALS_ACCESS_KEY=<access-key> -DAWS_S3_CREDENTIALS_SECRET_KEY=<secret-key> "it:test-only *MercuryS3Spec"
  * </pre>
  * If none of the above environment variables are provided, then everything defaults to localhost services which can be achieved by first starting up "docker-compose up" before "it:test-only *MercuryS3Spec"
  * @param env ExecutionEnv For asynchronous testing
  */
class MercuryS3Spec(implicit env: ExecutionEnv) extends Specification {
  trait Context extends ComposableAround {
    var s3: S3 = _

    override def around[R: AsResult](r: => R): Result = try {
      s3 = uk.gov.homeoffice.mercury.boot.configuration.S3(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))

      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      Try { s3.s3Client.shutdown() }
    }
  }

  "Mercury" should {
    "publish file to S3" in new Context {
      val file = new File("src/it/resources/s3/test-file.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case Push.Completed(fileName, _, _) => fileName mustEqual file.getName
      }.awaitFor(30 seconds)

      /*s3.push(file1.getName, file1, Some(AES256("secret key"))).map { push =>
        println(s"===> $push")
      }*/
    }
  }
}