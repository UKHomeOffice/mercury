package uk.gov.homeoffice.mercury.boot.configuration

import java.net.URL
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import uk.gov.homeoffice.aws.s3.{S3, S3Client}
import uk.gov.homeoffice.configuration.HasConfig

object S3 extends HasConfig {
  def apply(clientConfiguration: ClientConfiguration = new ClientConfiguration) = {
    val s3Host = new URL(config.getString("aws.s3.uri"))

    val accessKey = config.getString("aws.s3.credentials.access-key")
    val secretKey = config.getString("aws.s3.credentials.secret-key")

    implicit val s3Client = new S3Client(s3Host, new BasicAWSCredentials(accessKey, secretKey))(clientConfiguration)

    new S3(config.getString("aws.s3.buckets.mercury"))
  }
}