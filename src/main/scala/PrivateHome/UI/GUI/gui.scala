package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.{data, settings}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{extractRequest, handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.{ServerSettings, WebSocketSettings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object gui {
  implicit val actorSystem: ActorSystem = ActorSystem("system")
  val route: Route = path("ws") {
    extractRequest { request =>
      handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
    }
  }


  Http().newServerAt("0.0.0.0",settings.websocket.port).adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveMaxIdle(1.second))).bind(route)


}

