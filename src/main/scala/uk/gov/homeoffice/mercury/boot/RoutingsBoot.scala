package uk.gov.homeoffice.mercury.boot

import uk.gov.homeoffice.akka.http.{AkkaHttpBoot, AkkaHttpConfig}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.routing.{HealthCheckRouting, WelcomeRouting}

trait RoutingsBoot extends AkkaHttpBoot with HasConfig {
  this: App =>

  boot(WelcomeRouting, HealthCheckRouting)(AkkaHttpConfig(port = config.int("akka.http.port", 9100)))
}