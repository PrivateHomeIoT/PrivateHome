package PrivateHome.UI.GUI

import java.security.{KeyStore, SecureRandom}

import PrivateHome.UI.Websocket.websocket
import PrivateHome.settings
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import scala.concurrent.duration._

object gui {
  implicit val actorSystem: ActorSystem = ActorSystem("system")
  val password: Array[Char] = "password".toCharArray //TODO: Read from Settings

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystore = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12") //ToDo: Reads from filesystem with Settings config

  require(keystore != null, "Keystore required! No Keystore in resources.")
  ks.load(keystore, password)

  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)


  val websocketRoute: Route = path(settings.websocket.path) {
    extractRequest { request =>
      handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
    }
  }

  Http().newServerAt("0.0.0.0",settings.websocket.port).adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveMaxIdle(1.second))).enableHttps(https).bind(websocketRoute)

 val httpRoute: Route =  {
     concat(
       pathEndOrSingleSlash {
         redirect("/Devices.html", StatusCodes.PermanentRedirect)
       },
       getFromResourceDirectory(settings.http.path)
     )
  }
  Http().newServerAt("0.0.0.0",settings.http.port).enableHttps(https).bind(httpRoute)

}

