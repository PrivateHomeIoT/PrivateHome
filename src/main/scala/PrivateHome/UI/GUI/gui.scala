package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.data
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
  val defaultSettings: ServerSettings = ServerSettings(actorSystem)
  val customSettings: WebSocketSettings = defaultSettings.websocketSettings.withPeriodicKeepAliveMaxIdle(1.second)
  val customServerSettings: ServerSettings = defaultSettings.withWebsocketSettings(customSettings)
  val route: Route = path("ws") {
    extractRequest { request =>
      handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
    }
  }

  Http().bindAndHandle(route, "0.0.0.0", data.settings("port"), settings = customServerSettings).onComplete {

    case Success(binding) => println(s"Listening on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}/ws")
    case Failure(exception) => throw exception

  }

}

