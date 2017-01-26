package uk.gov.homeoffice.mercury

import akka.actor.Props
import akka.testkit.TestActorRef
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results._
import play.api.routing.sird._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.{ActorExpectations, ActorSystemSpecification}
import uk.gov.homeoffice.aws.s3.{S3, S3ServerEmbedded}
import uk.gov.homeoffice.aws.sqs.{Queue, SQS, SQSServerEmbedded}
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  trait Context extends ActorSystemContext with ActorExpectations with SQSServerEmbedded with S3ServerEmbedded {
    val queue = create(new Queue("test-queue"))

    val bucket = "test-bucket"
  }

  "Mercury subscription" should {
    "receive an AWS SQS message as plain text" in new Context {
      val message = "A plain text message"

      val subscriberActor = system actorOf Props(new MercuryActor(new SQS(queue), mock[S3], mock[WebService]))
      subscriberActor ! createMessage(message)

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }

  "Mercury S3" should {
    "pull in a file" in new Context {
      val actor = TestActorRef {
        new MercuryActor(new SQS(queue), new S3(bucket), mock[WebService])
      }
    }
  }

  "Mercury publication" should {
    "publish a received plain text AWS SQS message" in new Context {
      routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
          // I expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some("text/plain"), tempFile)) = request.body.files
          Ok
        }
      } { webService =>
        val message = "A plain text message"

        val actor = TestActorRef {
          new MercuryActor(new SQS(queue), mock[S3], webService)
        }

        actor.underlyingActor publish createMessage(message) must beEqualTo("caseRef").await
      }
    }

    "fail to publish a received plain text AWS SQS message because endpoint is not available" in new Context {
      routes { case _ => Action(BadGateway) } { webService =>
        val message = "A plain text message"

        val actor = TestActorRef {
          new MercuryActor(new SQS(queue), mock[S3], webService)
        }

        actor.underlyingActor publish createMessage(message) must throwAn[Exception](message = "502, Bad Gateway").await
      }
    }

    "publish a received plain text AWS SQS message with an associated attachment" in new Context {
      todo

      /*routes {
        case POST(p"/alfresco/s/cmis/p/CTS/Cases/children") => Action(parse.multipartFormData) { request =>
          // I expect one file of type text/plain
          val Seq(FilePart("email", "email.txt", Some("text/plain"), tempFile)) = request.body.files
          Ok
        }
      } { webService =>
        val message = "A plain text message"

        val actor = TestActorRef {
          new MercuryActor(new Subscriber(queue), webService)
        }

        actor.underlyingActor publish createMessage(message) must beEqualTo("caseRef").await
      }*/
    }
  }

  "AWS SQS messages via subscription" should {
    "be captured as plain text" in new Context {
      implicit val listeners = Seq(testActor)

      val message = "A plain text message"

      val sqs = new SQS(queue)
      val subscriberActor = system actorOf Props(new MercuryActor(sqs, mock[S3], mock[WebService]))

      sqs publish message

      eventuallyExpectMsg[String] {
        case response => response == s"Handled message: $message"
      }
    }
  }
}