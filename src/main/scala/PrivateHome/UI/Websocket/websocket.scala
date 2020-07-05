package PrivateHome.UI.Websocket

import PrivateHome.UI._
import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import com.fasterxml.jackson.core.JsonParseException
import org.json4s
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._


object websocket {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private var browserConnections: List[TextMessage => Unit] = List()

  def listen(): Flow[Message, Message, NotUsed] = {
    val inbound: Sink[Message, Any] = Sink.foreach(msg => {
      try {
        val msgText = msg.asTextMessage.getStrictText
        val json = parse(msgText)
        val commandType = json \ "Command"
        val args = json \ "Args"
        commandType.extract[String] match {
          case "on" => uiControl.receiveCommand(args.extract[commandOn])
          case "off" => uiControl.receiveCommand(args.extract[commandOff])
          case "getDevices" => uiControl.receiveCommand(args.extract[commandGetDevices])
          case "settingsMain" => uiControl.receiveCommand(args.extract[commandSettingsMain])
          case "settingsDevice" => uiControl.receiveCommand(args.extract[commandSettingsDevice])
          case e => val json: json4s.JObject = ("error" -> "Unknown Command") ~ ("command" -> e) ~ ("msg" -> msgText); sendMsg(json)
        }
      }
      catch {
        case e: JsonParseException => sendMsg(("error" -> "JsonParseException") ~ ("exception" -> e.toString))
        case e => sendMsg(("error" -> e.getCause.toString) ~ ("exception" -> e.toString))
      }


    })

    val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

    Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {
      browserConnections ::= outboundMat.offer
      NotUsed
    })
  }

  def sendMsg(msg: json4s.JObject) {
    for (connection <- browserConnections) connection(TextMessage.Strict(compact(render(msg))))
  }

  def sendMsg(text: String): Unit = {
    for (connection <- browserConnections) connection(TextMessage.Strict(text))
  }
}




