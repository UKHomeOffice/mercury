package uk.gov.homeoffice.mercury.routing

import uk.gov.homeoffice.akka.http.Routing

object WelcomeRouting extends WelcomeRouting

trait WelcomeRouting extends Routing {
  val route =
    pathEndOrSingleSlash {
      get {
        complete {
          "Welcome to Mercury"
        }
      }
    }
}