package uk.gov.homeoffice.mercury

import java.net.URL
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.web.WebService

/**
  * As this spec connects to an internal system, running locally will probably require VPN.
  * @param env ExecutionEnv For asynchronous testing
  */
class HocsSpec(implicit env: ExecutionEnv) extends Specification {
  "Hocs client" should {
    /**
      * Request example:
      * <pre>
      * curl -v -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
      *   "username" : "emailapiuser",
      *   "password" : "Password1"
      * }' "https://dev.hocs.homeoffice.gov.uk/alfresco/s/api/login"
      * </pre>
      * And response example:
      * <pre>
      * {
      *   "data": {
      *     "ticket":"TICKET_654c5fdd26d7ef38a03196f932a62dc1c053ccfe"
      *   }
      * }
      * </pre>
      */
    "login and be given a login token/ticket" in {
      val webService = WebService(new URL("https://dev.hocs.homeoffice.gov.uk"))

      val credentials = Json.obj(
        "username" -> "emailapiuser",
        "password" -> "Password1"
      )

      webService endpoint "/alfresco/s/api/login" post credentials must beLike[WSResponse] {
        case response =>
          response.status mustEqual OK
          (response.json \ "data" \ "ticket").as[String] must startWith("TICKET")
      }.await
    }
  }
}