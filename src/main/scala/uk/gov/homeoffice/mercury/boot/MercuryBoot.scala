package uk.gov.homeoffice.mercury.boot

import scala.language.postfixOps
import akka.actor.ActorSystem
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.feature.FeatureSwitch
import uk.gov.homeoffice.mercury.boot.configuration.{HocsCredentials, HocsWebService, S3, SQS}
import uk.gov.homeoffice.mercury.sqs.{MercuryActor => MercurySQSActor}
import uk.gov.homeoffice.mercury.s3.{MercuryActor => MercuryS3Actor}

trait MercuryBoot extends HasConfig with FeatureSwitch {
  this: App =>

  val system = ActorSystem("mercury-actor-system", config)
  sys addShutdownHook system.terminate()

  withFeature("aws.sqs.events-enabled") {
    system.actorOf(MercurySQSActor.props(SQS(), S3(), HocsCredentials(), HocsWebService()), name = "mercury-actor")
  } orElse withFeature("aws.s3.polling-enabled") {
    system.actorOf(MercuryS3Actor.props(S3(), HocsCredentials(), HocsWebService()), name = "mercury-actor")
  }
}