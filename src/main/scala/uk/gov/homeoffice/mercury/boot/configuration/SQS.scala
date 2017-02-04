package uk.gov.homeoffice.mercury.boot.configuration

import java.net.URL
import com.amazonaws.auth.BasicAWSCredentials
import uk.gov.homeoffice.aws.sqs.{Queue, QueueCreation, SQS, SQSClient}
import uk.gov.homeoffice.configuration.HasConfig

object SQS extends QueueCreation with HasConfig {
  val sqsHost = new URL(config.getString("aws.sqs.uri"))

  val accessKey = config.getString("aws.sqs.credentials.access-key")
  val secretKey = config.getString("aws.sqs.credentials.secret-key")

  implicit lazy val sqsClient = new SQSClient(sqsHost, new BasicAWSCredentials(accessKey, secretKey))

  def apply() = new SQS(create(new Queue(config.getString("aws.sqs.queues.mercury"))))
}