package uk.gov.homeoffice.mercury

import scala.concurrent.duration._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryAuthorizationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito with NoLanguageFeatures {
  "Mercury authorization" should {
    "fail because of missing user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("", password) must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of invalid user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("wrong", password) must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of missing password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "") must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "fail because of invalid password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "wrong") must throwAn[Exception](message = "401, Unauthorized").awaitFor(30 seconds)
      }
    }

    "pass resulting in a given token" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, password) must beLike[WebService] {
          case webService: WebService with Authorization => webService.token mustEqual ticket
        }.awaitFor(30 seconds)
      }
    }
  }
}