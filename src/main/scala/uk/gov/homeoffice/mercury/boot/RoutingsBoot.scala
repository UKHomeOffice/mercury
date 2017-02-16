package uk.gov.homeoffice.mercury.boot

import uk.gov.homeoffice.akka.http.{AkkaHttpBoot, AkkaHttpConfig}
import uk.gov.homeoffice.mercury.routing.{HealthCheckRouting, WelcomeRouting}

trait RoutingsBoot extends AkkaHttpBoot {
  this: App =>

  boot(WelcomeRouting, HealthCheckRouting)(AkkaHttpConfig())
}