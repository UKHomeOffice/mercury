package uk.gov.homeoffice.mercury

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.{Ok, Unauthorized}
import play.api.mvc.{Action, Handler, Request, RequestHeader}
import play.api.routing.sird._
import uk.gov.homeoffice.aws.s3.S3ServerEmbedded
import uk.gov.homeoffice.aws.sqs.SQSServerEmbedded

trait MercuryServicesContext extends SQSServerEmbedded with S3ServerEmbedded {
  val userName = "userName"
  val password = "password"
  val credentials = Credentials(userName, password)
  val ticket = "TICKET"

  val authorizeRoute: PartialFunction[RequestHeader, Handler] = {
    case POST(p"/alfresco/s/api/login") => Action(parse.json) { implicit request =>
      (param("username"), param("password")) match {
        case (Some(`userName`), Some(`password`)) => Ok(Json.obj("data" -> Json.obj("ticket" -> ticket)))
        case _ => Unauthorized
      }
    }
  }

  val authorizeCheck: PartialFunction[RequestHeader, Handler] = {
    case rh if !rh.getQueryString("alf_ticket").contains(ticket) => Action {
      Unauthorized
    }
  }

  def param(key: String)(implicit request: Request[JsValue]): Option[String] = (request.body \ key).asOpt[String]
}