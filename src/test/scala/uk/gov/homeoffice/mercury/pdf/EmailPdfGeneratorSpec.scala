package uk.gov.homeoffice.mercury.pdf

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import uk.gov.homeoffice.mercury.email.EmailContents

class EmailPdfGeneratorSpec extends Specification {

  private trait Context extends Scope {
    val pdfGenerator = EmailPdfGenerator
  }

  "EmailPdfGenerator" should {
    "Generate a pdf correctly" in new Context {
      val email = EmailContents("from@email.com", "to@email.com", "Test Subject", "Some Content\r\nOver here", "")
      val pdf = pdfGenerator.generatePdf(email)
      pdf.exists() must beTrue
      pdf.length() mustEqual 1215
      pdf.delete()
    }
  }
}
