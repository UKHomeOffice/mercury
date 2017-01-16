package uk.gov.homeoffice.mercury

import uk.gov.homeoffice.akka.http.{AkkaHttpBoot, AkkaHttpConfig}
import uk.gov.homeoffice.mercury.akka.http.HealthCheckRouting

object Boot extends App with AkkaHttpBoot with HealthCheckRouting {
  implicit val config = AkkaHttpConfig()

  boot(HealthCheckRouting)
}