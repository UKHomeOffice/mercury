package uk.gov.homeoffice.mercury.boot.configuration

import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.mercury.Credentials

object HocsCredentials extends HasConfig {
  def apply() = {
    val userName = config.getString("web-services.hocs.credentials.user-name")
    val password = config.getString("web-services.hocs.credentials.password")

    Credentials(userName, password)
  }
}