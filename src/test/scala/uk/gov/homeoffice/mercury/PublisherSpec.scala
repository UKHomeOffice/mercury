package uk.gov.homeoffice.mercury


import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.web.WebServiceSpecification

class PublisherSpec(implicit env: ExecutionEnv) extends Specification with WebServiceSpecification {
  "Publisher" should {
    "publish email without any attachments" in routes {
      case POST(p"") => Action {
        Ok("blah")
      }
    } { webService =>
      ok
    }

    "publish email with an attachment" in {
      todo
    }

    "publish email with two attachments" in {
      todo
    }
  }
}