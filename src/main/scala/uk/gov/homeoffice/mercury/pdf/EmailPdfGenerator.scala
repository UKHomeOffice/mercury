package uk.gov.homeoffice.mercury.pdf

import uk.gov.homeoffice.mercury.email.EmailContents

import scala.io.Source

object EmailPdfGenerator extends EmailPdfGenerator

class EmailPdfGenerator extends PdfGenerator {

  private val pdfTemplate = Source.fromInputStream(getClass.getResourceAsStream("/pdf-template.html")).mkString

  def generatePdf(email: EmailContents) = {
    val doc = pdfTemplate.replace("@@From@@", email.from)
      .replace("@@To@@", email.to)
      .replace("@@Subject@@", email.subject)
      .replace("@@Body@@", email.txt.replaceAll("\r\n", "<br/>"))

    super.generatePdf(doc)
  }

}
