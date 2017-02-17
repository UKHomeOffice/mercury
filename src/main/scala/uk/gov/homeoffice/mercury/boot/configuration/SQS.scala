package uk.gov.homeoffice.mercury.boot.configuration

import java.net.URL
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import uk.gov.homeoffice.aws.sqs.{Queue, QueueCreation, SQSClient}
import uk.gov.homeoffice.configuration.HasConfig

object SQS extends QueueCreation with HasConfig {
  def apply(clientConfiguration: ClientConfiguration = new ClientConfiguration) = {
    val sqsHost = new URL(config.getString("aws.sqs.uri"))

    val accessKey = config.getString("aws.sqs.credentials.access-key")
    val secretKey = config.getString("aws.sqs.credentials.secret-key")

    implicit lazy val sqsClient = new SQSClient(sqsHost, new BasicAWSCredentials(accessKey, secretKey))(clientConfiguration)

    new uk.gov.homeoffice.aws.sqs.SQS(create(new Queue(config.getString("aws.sqs.queues.mercury"))))
  }
}