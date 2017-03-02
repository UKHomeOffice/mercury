package uk.gov.homeoffice.mercury

import java.io.File
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
import uk.gov.homeoffice.aws.s3.S3.ResourcesKey
import uk.gov.homeoffice.aws.s3.{Resource, S3}
import uk.gov.homeoffice.aws.sqs.{Queue, SQS}
import uk.gov.homeoffice.web.{WebService, WebServiceSpecification}

class MercuryActorSpec(implicit env: ExecutionEnv) extends Specification with ActorSystemSpecification with WebServiceSpecification with Mockito {
  spec =>

  trait Context extends ActorSystemContext with ActorExpectations with MercuryServicesContext {
    implicit val listeners = Seq(testActor)

    val queue = create(new Queue("test-queue"))
  }

  /**
    * Due to an odd way the Fake S3 service works, we have to filter "groups" within Fake S3
    * @param s3 S3
    * @param webService WebService with Authorization
    * @return Mercury That is authorized against relevant web service
    */
  def mercuryAuthorized(s3: S3, webService: WebService with Authorization) = new Mercury(s3, webService) {
    override def groupByTopDirectory(resources: Seq[Resource]): Map[ResourcesKey, Seq[Resource]] =
      super.groupByTopDirectory(resources).filterNot(_._1.contains("."))
  }

  "Mercury actor" should {
    "receive a SQS message as plain text, but not be in an authorized state to publish it" in new Context {
      routes(PartialFunction.empty) { webService =>
        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), s3, credentials, webService) {
              override def preStart(): Unit = () // Avoid attempt to automatically authorize
            }
          }
        }

        mercuryActor ! createMessage("blah")

        eventuallyExpectMsg[String] {
          case response => response == "Received a message but Mercury is not authorized to perform publication"
        }
      }
    }

    "receive a SQS message but not have anything to publish" in new Context {
      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action {
          Ok
        }
      }) { webService =>
        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), s3, credentials, webService) {
              override def mercuryAuthorized(s3: S3, webService: WebService with Authorization): Mercury = spec.mercuryAuthorized(s3, webService)
            }
          }
        }

        eventuallyExpectMsg[Authorized.type]()

        mercuryActor ! createMessage("blah")

        eventuallyExpectMsg[Seq[Publication]] {
          case response => response == Nil
        }
      }
    }

    "receive a SQS message and be notified when the associated resource has been published" in new Context {
      val file = new File(s"$s3Directory/test-file.txt")
      val fileName = file.getName

      routes(authorizeRoute orElse authorizeCheck orElse {
        case POST(p"/alfresco/s/homeoffice/cts/autoCreateDocument") => Action(parse.multipartFormData) { request =>
          // Expect one file of type text/plain
          val Seq(FilePart("file", `fileName`, Some("text/plain; charset=UTF-8"), _)) = request.body.files
          Ok
        }
      }) { webService =>
        val mercuryActor = TestActorRef {
          Props {
            new MercuryActor(new SQS(queue), s3, credentials, webService) {
              override def mercuryAuthorized(s3: S3, webService: WebService with Authorization): Mercury = spec.mercuryAuthorized(s3, webService)
            }
          }
        }

        eventuallyExpectMsg[Authorized.type]()

        s3.push(s"folder/$fileName", file).foreach { _ =>
          mercuryActor ! createMessage("folder")
        }

        eventuallyExpectMsg[Seq[Publication]] {
          case response => response == Seq(Publication("caseRef"))
        }
      }
    }
  }
}