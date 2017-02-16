package uk.gov.homeoffice.mercury.routing

import akka.http.scaladsl.model.StatusCodes._
import uk.gov.homeoffice.akka.http.Routing

object HealthCheckRouting extends HealthCheckRouting

trait HealthCheckRouting extends Routing {
  val route =
    path("health") {
      get {
        complete(OK -> "ok")
      }
    }
}