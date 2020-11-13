package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.settings
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._

object gui {
  implicit val actorSystem: ActorSystem = ActorSystem("system")
  val websocketRoute: Route = path(settings.websocket.path) {
    extractRequest { request =>
      handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
    }
  }

  Http().newServerAt("0.0.0.0",settings.websocket.port).adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveMaxIdle(1.second))).bind(websocketRoute)

 val httpRoute: Route =  {
     concat(
       pathEndOrSingleSlash {
         redirect("/Devices.html", StatusCodes.PermanentRedirect)
       },
       getFromResourceDirectory(settings.http.path)
     )
  }
  Http().newServerAt("0.0.0.0",settings.http.port).bind(httpRoute)

}

