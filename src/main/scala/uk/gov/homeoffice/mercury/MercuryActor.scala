package uk.gov.homeoffice.mercury

import java.io.ByteArrayInputStream
import scala.concurrent.ExecutionContext.Implicits.global // TODO
import scala.concurrent.Future
import akka.actor.ActorRef
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters.fromInputStream
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import uk.gov.homeoffice.aws.sqs.Message
import uk.gov.homeoffice.aws.sqs.subscribe.Subscriber
import uk.gov.homeoffice.mercury.MediaTypes.Implicits._
import uk.gov.homeoffice.web.WebService

class MercuryActor(subscriber: Subscriber, webService: WebService)(implicit listeners: Seq[ActorRef] = Seq.empty[ActorRef]) extends uk.gov.homeoffice.aws.sqs.subscribe.SubscriberActor(subscriber) {
  // TODO - Future[String]? What do we really want to get back? Case ref, email address ...
  val publish: Message => Future[String] = { m =>
    val email = fromInputStream(() => new ByteArrayInputStream("Email to: Jimmy".getBytes))

    val emailFilePart = FilePart("email", "email.txt", Some(`text/plain`), email)

    webService endpoint "/alfresco/s/cmis/p/CTS/Cases/children" post Source(List(emailFilePart)) flatMap { response =>
      new Status(response.status) match {
        case Ok => Future successful "caseRef" // TODO
        case status => Future failed new Exception("TODO") // TODO
      }
    }
  }

  override def receive: Receive = {
    case m: Message => sender() ! s"Handled message: ${m.content}"
  }
}