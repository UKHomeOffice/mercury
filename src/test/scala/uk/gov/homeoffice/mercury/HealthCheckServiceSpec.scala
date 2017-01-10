package uk.gov.homeoffice.mercury

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{MustMatchers, WordSpec}
import org.specs2.specification.Scope

class HealthCheckServiceSpec extends WordSpec with MustMatchers with ScalatestRouteTest {

  trait TestContext extends Scope {
    val healthCheckService = new HealthCheckService {
      def actorRefFactory = system
    }
  }

  "healthz endpoint" should {
    "always return a 200 status" in new TestContext {
      Get("/healthz") ~> healthCheckService.routes ~> check {
        status.intValue mustEqual 200
      }
    }
  }

  "readiness endpoint" should {
    "always return a 200 status" in new TestContext {
      Get("/readiness") ~> healthCheckService.routes ~> check {
        status.intValue mustEqual 200
      }
    }
  }

}