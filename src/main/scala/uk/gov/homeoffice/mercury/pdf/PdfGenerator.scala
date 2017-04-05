package uk.gov.homeoffice.mercury.pdf

import java.io.{File, FileOutputStream}

import org.xhtmlrenderer.pdf.ITextRenderer

trait PdfGenerator {

  def generatePdf(doc: String): File = {
    val renderer = new ITextRenderer()
    renderer.setDocumentFromString(doc)
    renderer.layout()
    val file = File.createTempFile("email-pdf", ".pdf")
    val fos = new FileOutputStream(file)
    renderer.createPDF(fos)
    fos.close()
    file
  }
}
