package uk.gov.homeoffice.mercury.email

import java.io.{File, FileOutputStream}

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class EmailParserSpec extends Specification {

  private trait Context extends Scope {
    val emailParser = EmailParser
  }

  "EmailParser" should {
    "parse Email correctly" in new Context {
      val email: EmailContents = emailParser.parse(getClass.getResourceAsStream("/s3/test-email.txt"))

      email.subject mustEqual "test email"
      email.txt mustEqual "This is a test email going to ses\n\n\nincluding pdf attachment\n\nPlease ensure that any communication with Home Office Digital is via an official account ending with digital.homeoffice.gov.uk or homeoffice.gsi.gov.uk. This email and any files transmitted with it are private and intended solely for the use of the individual or entity to whom they are addressed. If you have received this email in error please return it to the address it came from telling them it is not for you and then delete it from your system. Communications via the digital.homeoffice.gov.uk domain may be automatically logged, monitored and/or recorded for legal purposes. This email message has been swept for computer viruses."
      email.html mustEqual "<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=Windows-1252\">\n<style type=\"text/css\" style=\"display:none;\"><!-- P {margin-top:0;margin-bottom:0;} --></style>\n</head>\n<body dir=\"ltr\">\n<div id=\"divtagdefaultwrapper\" style=\"font-size:12pt;color:#000000;font-family:Calibri,Arial,Helvetica,sans-serif;\" dir=\"ltr\">\n<p>This is a test email going to ses</p>\n<p><br>\n</p>\n<p>including pdf attachment</p>\n</div>\n<br>\nPlease ensure that any communication with Home Office Digital is via an official account ending with digital.homeoffice.gov.uk or homeoffice.gsi.gov.uk. This email and any files transmitted with it are private and intended solely for the use of the individual\n or entity to whom they are addressed. If you have received this email in error please return it to the address it came from telling them it is not for you and then delete it from your system. Communications via the digital.homeoffice.gov.uk domain may be automatically\n logged, monitored and/or recorded for legal purposes. This email message has been swept for computer viruses.\n</body>\n</html>"
      email.attachments.size mustEqual 1
      email.attachments(0).contentType mustEqual "application/pdf"
      email.attachments(0).name mustEqual "The-Limited-Company-Contractor’s-2017-New-Year-Resolutions.pdf"

      val attachment = File.createTempFile("email-pdf", ".pdf")
      val fos = new FileOutputStream(attachment)
      email.attachments(0).body.writeTo(fos)
      fos.close()
      attachment.length() mustEqual 1615723
      attachment.delete()
    }
  }
}
