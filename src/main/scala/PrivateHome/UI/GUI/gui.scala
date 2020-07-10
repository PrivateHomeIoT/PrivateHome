package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.{data, settings}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object gui {

  var run = true

  implicit val actorSystem: ActorSystem = ActorSystem("system")

  val route: Route = path("ws") {
    handleWebSocketMessages(websocket.listen())
  }

  Http().bindAndHandle(route, "0.0.0.0", settings.http.port).onComplete {

    case Success(binding) => println(s"Listening on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}.")
    case Failure(exception) => throw exception

  }

}

