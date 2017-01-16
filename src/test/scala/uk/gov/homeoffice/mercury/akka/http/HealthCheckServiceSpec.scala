package uk.gov.homeoffice.mercury.akka.http

import akka.http.scaladsl.model.StatusCodes._
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.http.RouteSpecification

class HealthCheckServiceSpec extends Specification with RouteSpecification with HealthCheckRouting {
  "Health endpoint" should {
    "show that everything is ok" in {
      Get("/health") ~> route ~> check {
        status mustEqual OK
      }
    }
  }
}