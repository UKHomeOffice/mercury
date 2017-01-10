package uk.gov.homeoffice.mercury

import akka.http.scaladsl.server.Directives._

trait HealthCheckService {
  val routes =
    path("readiness") {
      get {
        complete("ok")
      }
    } ~
      path("healthz") {
        get {
          complete("ok")
        }
      }

}