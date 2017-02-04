package uk.gov.homeoffice.mercury.boot.configuration

import java.net.URL
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.web.WebService

object HocsWebService extends HasConfig {
  val webServiceHost = new URL(config.getString("web-services.hocs.uri"))

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val wsClient = AhcWSClient()

  sys addShutdownHook {
    system.terminate()
    wsClient.close()
  }

  def apply() = new WebService(webServiceHost, wsClient)
}