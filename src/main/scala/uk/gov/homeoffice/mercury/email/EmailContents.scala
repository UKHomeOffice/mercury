package uk.gov.homeoffice.mercury.email

import org.apache.james.mime4j.dom.BinaryBody

case class EmailContents(from: String, to: String, subject: String, txt: String, html: String, attachments: Seq[EmailAttachment])

case class EmailAttachment(contentType: String, name: String, body: BinaryBody)