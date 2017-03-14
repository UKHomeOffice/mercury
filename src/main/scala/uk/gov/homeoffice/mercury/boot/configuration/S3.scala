package uk.gov.homeoffice.mercury.boot.configuration

import java.net.URL
import scala.util.Try
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.S3ClientOptions
import uk.gov.homeoffice.aws.s3.S3Client
import uk.gov.homeoffice.configuration.HasConfig

object S3 extends HasConfig {
  def apply(clientConfiguration: ClientConfiguration = new ClientConfiguration) = {
    val s3Host = new URL(config.getString("aws.s3.uri"))

    val accessKey = config.getString("aws.s3.credentials.access-key")
    val secretKey = config.getString("aws.s3.credentials.secret-key")

    implicit val s3Client = new S3Client(s3Host, new BasicAWSCredentials(accessKey, secretKey))(clientConfiguration)
    s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())

    Try {
      config.getString("aws.s3.region")
    } map { region =>
      s3Client.setRegion(Region.getRegion(Regions.fromName(region)))
    }

    val mercuryBucket = config.getString("aws.s3.buckets.mercury")

    Try {
      // We should be able to ignore any error message if this fails as the bucket should have already been created.
      s3Client.createBucket(mercuryBucket)
    }

    new uk.gov.homeoffice.aws.s3.S3(mercuryBucket)
  }
}