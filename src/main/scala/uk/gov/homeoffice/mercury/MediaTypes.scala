package uk.gov.homeoffice.mercury

import akka.http.scaladsl.model.MediaType

object MediaTypes {
  object Implicits {
    implicit val mediaType2String: MediaType => String = _.value
  }
}