package uk.gov.homeoffice.mercury.boot

import akka.actor.{PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import uk.gov.homeoffice.akka.cluster.ClusterActorSystem
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.feature.FeatureSwitch
import uk.gov.homeoffice.mercury.boot.configuration.{HocsCredentials, HocsWebService, S3, SQS}
import uk.gov.homeoffice.mercury.sqs.{MercuryActor => MercurySQSActor}

import scala.language.postfixOps

/**
  * sbt '; set javaOptions += "-Dcluster.node=1"; run'
  * MERCURY_CLUSTER_SEED_NODE_PORT_2=2662 sbt '; set javaOptions ++= Seq("-Dcluster.node=2", "-Dakka.http.port=9200"); run'
  * MERCURY_CLUSTER_SEED_NODE_PORT_3=2663 sbt '; set javaOptions ++= Seq("-Dcluster.node=3", "-Dakka.http.port=9300"); run'
  */
trait MercuryBoot extends HasConfig with FeatureSwitch {
  this: App =>

  val system = ClusterActorSystem()
  sys addShutdownHook system.terminate()

  system.actorOf(singletonProps(MercurySQSActor.props(SQS(), S3(), HocsCredentials(), HocsWebService())), name = "mercury-actor")

  def singletonProps(p: Props) = try {
    ClusterSingletonManager.props(
      singletonProps = p,
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system))
  } catch {
    case t: Throwable =>
      error("Fatal error on startup", t)
      sys.exit(1)
  }
}