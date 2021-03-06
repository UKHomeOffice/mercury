package uk.gov.homeoffice.mercury.email

import java.io.InputStream
import javax.mail.Part

import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.codec.{DecodeMonitor, DecoderUtil}
import org.apache.james.mime4j.dom._
import org.apache.james.mime4j.message.DefaultMessageBuilder

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object EmailParser extends EmailParser

trait EmailParser {

  def parse(inputStream: InputStream): EmailContents = {
    val builder = new DefaultMessageBuilder()
    val message = builder.parseMessage(inputStream)
    val (txt, html, attachs) = parseMessage(message)
    message.dispose

    EmailContents(from = message.getFrom.get(0).getAddress,
      to = message.getTo.flatten().get(0).getAddress,
      subject = message.getSubject,
      txt = txt,
      html = html,
      attachments = attachs)
  }

  private def parseMessage(mimeMsg: Message) = {
    val txtBody: StringBuilder = new StringBuilder
    val htmlBody: StringBuilder = new StringBuilder
    val attachments: ListBuffer[Entity] = new ListBuffer[Entity]()

    def parseBodyParts(multipart: Multipart) {

      def parsePart(part: Entity) = part.getMimeType match {
        case "text/plain" =>
          val txt = getTxtPart(part)
          txtBody.append(txt)
        case "text/html" =>
          val html = getTxtPart(part)
          htmlBody.append(html)

        case _ => if (Part.ATTACHMENT == part.getDispositionType)
          attachments += part
      }

      val bodyParts = multipart.getBodyParts().asScala.toList
      bodyParts foreach { part =>
        parsePart(part)
        if (part.isMultipart) parseBodyParts(part.getBody.asInstanceOf[Multipart])
      }
    }

    extractParts(mimeMsg, txtBody, parseBodyParts)
    (txtBody.toString.trim, htmlBody.toString.trim,
      attachments.map { a =>
        EmailAttachment(a.getMimeType, DecoderUtil.decodeEncodedWords(a.getFilename, DecodeMonitor.SILENT), a.getBody.asInstanceOf[BinaryBody])
      })
  }

  private def getTxtPart(part: Entity): String = {
    val body = part.getBody().asInstanceOf[TextBody]
    val is = body.getInputStream
    val res = IOUtils.readLines(is).asScala.mkString("\n")
    is.close()
    res
  }

  private def extractParts(mimeMsg: Message, txtBody: StringBuilder,
                           parseBodyParts: Multipart => Unit): Any = {
    //If message contains many parts - parse all parts
    if (mimeMsg.isMultipart) {
      val multipart = mimeMsg.getBody.asInstanceOf[Multipart]
      parseBodyParts(multipart)
    } else {
      //If it's single part message, just get text body
      val text = getTxtPart(mimeMsg)
      txtBody.append(text)
    }
  }
}
