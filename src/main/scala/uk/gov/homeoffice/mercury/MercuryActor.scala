package uk.gov.homeoffice.mercury

import akka.actor.ActorRef
import uk.gov.homeoffice.aws.s3.S3
import uk.gov.homeoffice.aws.sqs.{Message, SQS, SQSActor}
import uk.gov.homeoffice.web.WebService

class MercuryActor(sqs: SQS, s3: S3, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends SQSActor(sqs) {
  // TODO - Remove as this has been moved
  /*val publish: Message => Future[String] = { m =>
    val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes))

    val emailFilePart = FilePart("email", "email.txt", Some(`text/plain`), email)

    webService endpoint "/alfresco/s/cmis/p/CTS/Cases/children" post Source(List(emailFilePart)) flatMap { response =>
      response.status match {
        case OK => Future successful "caseRef" // TODO
        case _ => Future failed new Exception(s"""Failed to publish email to "${webService.host}" because of: Http response status ${response.status}, ${response.statusText}""")
      }
    }
  }*/

  override def receive: Receive = {
    case m: Message => sender() ! s"Handled message: ${m.content}"
  }
}