package uk.gov.homeoffice.mercury.pdf

import org.apache.commons.io.IOUtils
import uk.gov.homeoffice.mercury.email.EmailContents

object EmailPdfGenerator extends EmailPdfGenerator

class EmailPdfGenerator extends PdfGenerator {

  private val pdfTemplate = getPdfTemplate()

  def generatePdf(email: EmailContents) = {
    val doc = pdfTemplate.replace("@@From@@", email.from)
      .replace("@@To@@", email.to)
      .replace("@@Subject@@", email.subject)
      .replace("@@Body@@", email.txt.replaceAll("\n", "<br/>"))

    super.generatePdf(doc)
  }

  private def getPdfTemplate() = {
    val pdfTemplateRes = getClass().getResourceAsStream("/pdf-template.html")
    val pdfTemplate = IOUtils.toString(pdfTemplateRes)
    pdfTemplateRes.close()
    pdfTemplate
  }
}
