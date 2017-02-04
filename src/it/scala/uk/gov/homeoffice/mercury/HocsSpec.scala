package uk.gov.homeoffice.mercury

import java.net.URL
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.boot.configuration.HocsCredentials
import uk.gov.homeoffice.web.WebService

/**
  * As this spec connects to an internal system, running locally will probably require VPN.
  * @param env ExecutionEnv For asynchronous testing
  */
class HocsSpec(implicit env: ExecutionEnv) extends Specification with HasConfig {
  "Hocs client" should {
    /**
      * Request example:
      * <pre>
      * curl -v -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
      *   "username" : "username",
      *   "password" : "password"
      * }' "<host>/alfresco/s/api/login"
      * </pre>
      * And response example:
      * <pre>
      * {
      *   "data": {
      *     "ticket":"TICKET"
      *   }
      * }
      * </pre>
      */
    "login and be given a login token/ticket" in {
      val webService = WebService(new URL(config.getString("web-services.hocs.uri")))

      webService endpoint "/alfresco/s/api/login" post HocsCredentials() must beLike[WSResponse] {
        case response =>
          response.status mustEqual OK
          (response.json \ "data" \ "ticket").as[String] must startWith("TICKET")
      }.await
    }
  }
}