package uk.gov.homeoffice.mercury.pdf

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class PdfGeneratorSpec extends Specification {

  private trait Context extends Scope {
    val pdfGenerator = new PdfGenerator {}
  }

  "PdfGenerator" should {
    "Generate a pdf correctly" in new Context {
      val pdf = pdfGenerator.generatePdf("<html><body><strong>Hello</strong> <em>world</em>!</body></html>")
      pdf.exists() must beTrue
      pdf.length() mustEqual 1264
      pdf.delete()
    }
  }
}
