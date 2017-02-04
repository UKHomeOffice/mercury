package uk.gov.homeoffice.mercury.boot

import java.net.URL
import scala.language.postfixOps
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient
import com.amazonaws.auth.BasicAWSCredentials
import uk.gov.homeoffice.aws.s3.{S3, S3Client}
import uk.gov.homeoffice.aws.sqs.{Queue, QueueCreation, SQS, SQSClient}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.{Login, MercuryActor}
import uk.gov.homeoffice.web.WebService

trait MercuryBoot extends HasConfig {
  this: App =>

  val system = ActorSystem("mercury-actor-system", config)
  sys addShutdownHook system.terminate()

  system.actorOf(MercuryActor.props(SQS(), S3(), Login(), WebService()), name = "mercury-actor")

  object SQS extends QueueCreation {
    val sqsHost = new URL(config.text("amazon.sqs.uri", "http://localhost:9324/queue"))

    val accessKey = config.text("aws.sqs.credentials.access-key", "x")
    val secretKey = config.text("aws.sqs.credentials.secret-key", "x")

    implicit lazy val sqsClient = new SQSClient(sqsHost, new BasicAWSCredentials(accessKey, secretKey))

    def apply() = new SQS(create(new Queue(config.text("aws.sqs.queues.mercury-queue", "mercury"))))
  }

  object S3 {
    val s3Host = new URL(config.text("aws.s3.uri", "http://0.0.0.0:4569"))

    val accessKey = config.text("aws.s3.credentials.access-key", "x")
    val secretKey = config.text("aws.s3.credentials.secret-key", "x")

    implicit val s3Client = new S3Client(s3Host, new BasicAWSCredentials(accessKey, secretKey))

    def apply() = new S3(config.text("aws.s3.bucket", "mercury-bucket"))
  }

  object Login {
    val userName = config.text("web-service.login.user-name", "emailapiuser")
    val password = config.text("web-service.login.password", "Password1")

    def apply() = new Login(userName, password)
  }

  object WebService {
    val webServiceHost = new URL(config.text("web-service.uri", "http://localhost:9500"))

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val wsClient = AhcWSClient()

    sys addShutdownHook {
      system.terminate()
      wsClient.close()
    }

    def apply() = new WebService(webServiceHost, wsClient)
  }
}