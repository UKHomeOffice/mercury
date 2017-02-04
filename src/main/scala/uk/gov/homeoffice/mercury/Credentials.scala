package uk.gov.homeoffice.mercury

import akka.http.scaladsl.model.MediaTypes._
import akka.util.{ByteString, CompactByteString}
import play.api.http.Writeable
import play.api.libs.json.Json
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._

case class Credentials(userName: String, password: String)

object Credentials {
  val credentialsToByteString: Credentials => ByteString = { c =>
    val credentials = Json.obj(
      "username" -> c.userName,
      "password" -> c.password
    )

    CompactByteString(Json.stringify(credentials).getBytes)
  }

  implicit val credentialsWriteable: Writeable[Credentials] = Writeable(credentialsToByteString, Some(`application/json`))
}