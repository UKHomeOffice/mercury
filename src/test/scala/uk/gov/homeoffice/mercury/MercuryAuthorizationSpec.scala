package uk.gov.homeoffice.mercury

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryAuthorizationSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification with Mockito {
  "Mercury" should {
    "fail authorization because of missing user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("", password) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail authorization because of invalid user name" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials("wrong", password) must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail authorization because of missing password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "fail authorization because of invalid password" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, "wrong") must throwAn[Exception](message = "401, Unauthorized").await
      }
    }

    "pass authorization resulting in a given token" in new MercuryServicesContext {
      routes(authorizeRoute) { implicit ws =>
        Mercury authorize Credentials(userName, password) must beLike[WebService] {
          case webService: WebService with Authorization => webService.token mustEqual ticket
        }.await
      }
    }
  }
}