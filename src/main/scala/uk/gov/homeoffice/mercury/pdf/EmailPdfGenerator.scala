package uk.gov.homeoffice.mercury.pdf

import java.io.File

import org.apache.commons.io.FileUtils
import uk.gov.homeoffice.mercury.email.EmailContents

object EmailPdfGenerator extends EmailPdfGenerator

class EmailPdfGenerator extends PdfGenerator {

  private val pdfTemplate = FileUtils.readFileToString(new File(getClass.getResource("/pdf-template.html").toURI))

  def generatePdf(email: EmailContents) = {
    val doc = pdfTemplate.replace("@@From@@", email.from)
      .replace("@@To@@", email.to)
      .replace("@@Subject@@", email.subject)
      .replace("@@Body@@", email.txt.replaceAll("\n", "<br/>"))

    super.generatePdf(doc)
  }

}
