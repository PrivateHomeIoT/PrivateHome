package PrivateHome.UI.Websocket

import PrivateHome.UI.{commandOff, commandOn, uiControl}
import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import com.fasterxml.jackson.core.JsonParseException
import org.json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._



object websocket {

  implicit val formats = DefaultFormats

  private var browserConnections: List[TextMessage => Unit] = List()

  def listen(): Flow[Message, Message, NotUsed] = {
    val inbound: Sink[Message, Any] = Sink.foreach(msg => {
      try {
      val msgText = msg.asTextMessage.getStrictText
      val json = parse(msgText)
      val commandtype = json \ "Command"
      val args = json \ "Args"
      commandtype.extract[String].toLowerCase match {
        case "on" => uiControl.receiveCommand(args.extract[commandOn])
        case "off" => uiControl.receiveCommand(args.extract[commandOff])
        case e => val json: json4s.JObject = ("error"-> "Unknow Command")~("command"-> e)~("msg"->msgText); sendMsg(json)
      }}
      catch {
        case e:JsonParseException => sendMsg(("error"->"JsonParseExeption")~("exeption"-> e.toString ))
      }


    })

    val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

    Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {
      browserConnections ::= outboundMat.offer
      NotUsed
    })
  }

  def sendMsg(text: String): Unit = {
    for (connection <- browserConnections) connection(TextMessage.Strict(text))
  }

  def sendMsg(msg: json4s.JObject) {
    for (connection <- browserConnections) connection(TextMessage.Strict(compact(render(msg))))
  }
}




