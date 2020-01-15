package PrivateHome.UI.Websocket

import PrivateHome.UI.{commandOff, commandOn, uiControl}
import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import org.json4s._
import org.json4s.native.JsonMethods._

object websocket {
  private var browserConnections: List[TextMessage => Unit] = List()

  def listen(): Flow[Message, Message, NotUsed] = {
    val inbound: Sink[Message, Any] = Sink.foreach(msg => {
      val msgText = msg.asTextMessage.getStrictText
      println(msgText)
      try {
        val json = parse(msgText)
        val commandtype = json \"Command"
        val args = json \"Args"
        commandtype.toString.toLowerCase match {
          case "on" => uiControl.receiveCommand(args.extract[commandOn])
          case "off" => uiControl.receiveCommand(args.extract[commandOff])
          case _ => sendMsg(s""""error":"Unknown Command","Command","$msgText"""")
          }
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
}




