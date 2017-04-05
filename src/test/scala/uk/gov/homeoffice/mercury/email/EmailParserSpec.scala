package uk.gov.homeoffice.mercury.email

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class EmailParserSpec extends Specification {

  private trait Context extends Scope {
    val emailParser = EmailParser
  }

  "EmailParser" should {
    "parse Email correctly" in new Context {
      val email = emailParser.parse(getClass.getResourceAsStream("/s3/test-email.txt"))
      email.from mustEqual "Paul.Miles@digital.homeoffice.gov.uk"
      email.to mustEqual "dev@hocs.homeoffice.gov.uk"
      email.subject mustEqual "test email"
      email.txt mustEqual "This is a test email going to ses\r\n\r\n\r\nincluding pdf attachment\r\n\r\nPlease ensure that any communication with Home Office Digital is via an official account ending with digital.homeoffice.gov.uk or homeoffice.gsi.gov.uk. This email and any files transmitted with it are private and intended solely for the use of the individual or entity to whom they are addressed. If you have received this email in error please return it to the address it came from telling them it is not for you and then delete it from your system. Communications via the digital.homeoffice.gov.uk domain may be automatically logged, monitored and/or recorded for legal purposes. This email message has been swept for computer viruses."
      email.html mustEqual "<html>\r\n<head>\r\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=Windows-1252\">\r\n<style type=\"text/css\" style=\"display:none;\"><!-- P {margin-top:0;margin-bottom:0;} --></style>\r\n</head>\r\n<body dir=\"ltr\">\r\n<div id=\"divtagdefaultwrapper\" style=\"font-size:12pt;color:#000000;font-family:Calibri,Arial,Helvetica,sans-serif;\" dir=\"ltr\">\r\n<p>This is a test email going to ses</p>\r\n<p><br>\r\n</p>\r\n<p>including pdf attachment</p>\r\n</div>\r\n<br>\r\nPlease ensure that any communication with Home Office Digital is via an official account ending with digital.homeoffice.gov.uk or homeoffice.gsi.gov.uk. This email and any files transmitted with it are private and intended solely for the use of the individual\r\n or entity to whom they are addressed. If you have received this email in error please return it to the address it came from telling them it is not for you and then delete it from your system. Communications via the digital.homeoffice.gov.uk domain may be automatically\r\n logged, monitored and/or recorded for legal purposes. This email message has been swept for computer viruses.\r\n</body>\r\n</html>"
    }
  }
}
