package uk.gov.homeoffice.mercury

import uk.gov.homeoffice.akka.http.{AkkaHttpBoot, AkkaHttpConfig}

object Boot extends App with AkkaHttpBoot with HealthCheckRouting {
  implicit val config = AkkaHttpConfig()

  boot(HealthCheckRouting)
}