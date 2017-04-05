package uk.gov.homeoffice.mercury.email

case class EmailContents(from: String, to: String, subject: String, txt: String, html: String)