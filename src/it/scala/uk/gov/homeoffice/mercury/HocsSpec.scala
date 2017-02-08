package uk.gov.homeoffice.mercury

import java.io.ByteArrayInputStream
import java.net.URL
import scala.concurrent.duration._
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters.fromInputStream
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.FilePart
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.boot.configuration.HocsCredentials
import uk.gov.homeoffice.web.WebService

/**
  * As this spec connects to an internal system, running locally may require VPN.
  * This integration test can either run against a locally running Hocs Fake instance, or an appropriate test environment
  * Running against some test environment would require the following environment variables set as in the following example:
  * <pre>
  * export WEB_SERVICES_HOCS_URI="<host>"; export WEB_SERVICES_HOCS_LOGIN_USER_NAME="<userName>"; export WEB_SERVICES_HOCS_LOGIN_PASSWORD="<password>"; sbt it:test
  * </pre>
  * @param env ExecutionEnv For asynchronous testing
  */
class HocsSpec(implicit env: ExecutionEnv) extends Specification with HasConfig {
  trait Context extends Scope {
    val webService = WebService(new URL(config.getString("web-services.hocs.uri")))
  }

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
    "login and be given a login token/ticket" in new Context {
      webService endpoint "/alfresco/s/api/login" post HocsCredentials() must beLike[WSResponse] {
        case response =>
          response.status mustEqual OK
          (response.json \ "data" \ "ticket").as[String] must startWith("TICKET")
      }.awaitFor(5.seconds)
    }

    "login and be given a login token/ticket, and then submit a file" in new Context {
      webService endpoint "/alfresco/s/api/login" post HocsCredentials() flatMap { response =>
        val ticket = (response.json \ "data" \ "ticket").as[String]

        val email = fromInputStream(() => new ByteArrayInputStream("Blah blah blah blah".getBytes))
        val emailFilePart = FilePart("email", "email.txt", Some("text/plain"), email)

        webService endpoint "/alfresco/s/homeoffice/cts/autoCreateDocument" withQueryString ("alf_ticket" -> ticket) post Source(List(emailFilePart))
      } must beLike[WSResponse] {
        case response =>
          println(response.json)
          println(response.statusText)
          response.status mustEqual OK
      }.awaitFor(5.seconds)
    }
  }
}