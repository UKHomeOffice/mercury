package uk.gov.homeoffice.mercury.s3

import java.io.File

import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.{KMS, Push, S3, S3EncryptionClient}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.specs2.ComposableAround

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * sbt '; set javaOptions ++= Seq("-DAWS_S3_URI=<host>", "-DAWS_S3_CREDENTIALS_ACCESS_KEY=<access-key>", "-DAWS_S3_CREDENTIALS_SECRET_KEY=<secret-key>", "-DAWS_S3_KMS_KEY=<kms-key>"); it:test-only *MercuryS3Spec'
  * </pre>
  * If none of the above environment variables are provided, then everything defaults to localhost services which can be achieved by first starting up "docker-compose up" before "it:test-only *MercuryS3Spec"
 *
  * @param env ExecutionEnv For asynchronous testing
  */
class MercuryS3Spec(implicit env: ExecutionEnv) extends Specification with NoLanguageFeatures with HasConfig {
  trait Context extends ComposableAround {
    val s3: S3 = uk.gov.homeoffice.mercury.boot.configuration.S3(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))

    override def around[R: AsResult](r: => R): Result = try {
      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      s3.s3Client.asInstanceOf[S3EncryptionClient].shutdown()
    }
  }

  "Mercury" should {
    "publish resource to S3" in new Context {
      val file = new File("src/it/resources/s3/test-file.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case Push.Completed(fileName, _, _) => fileName mustEqual file.getName
      }.awaitFor(30 seconds)
    }

    "publish encrypted resource to S3" in new Context {
      skipped("Cannot test encryption against Fake S3")

      val file = new File("src/it/resources/s3/test-file.txt")

      s3.push(file.getName, file, Some(KMS(config.getString("aws.s3.kms-key")))) must beLike[Push] {
        case Push.Completed(fileName, _, _) => fileName mustEqual file.getName
      }.awaitFor(30 seconds)
    }
  }
}