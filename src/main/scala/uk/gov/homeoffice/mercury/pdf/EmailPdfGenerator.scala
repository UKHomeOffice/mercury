package uk.gov.homeoffice.mercury.pdf

import org.apache.commons.io.IOUtils
import uk.gov.homeoffice.mercury.email.EmailContents

import scala.xml.Utility.escape

object EmailPdfGenerator extends EmailPdfGenerator

class EmailPdfGenerator extends PdfGenerator {

  private val pdfTemplate = getPdfTemplate()

  def generatePdf(email: EmailContents) = {
    val doc = pdfTemplate.replace("@@From@@", escape(email.from))
      .replace("@@To@@", escape(email.to))
      .replace("@@Subject@@", escape(email.subject))
      .replace("@@Body@@", escape(email.txt).replaceAll("\n", "<br/>"))

    super.generatePdf(doc)
  }

  private def getPdfTemplate() = {
    val pdfTemplateRes = getClass().getResourceAsStream("/pdf-template.html")
    val pdfTemplate = IOUtils.toString(pdfTemplateRes)
    pdfTemplateRes.close()
    pdfTemplate
  }
}
