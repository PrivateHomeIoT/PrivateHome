package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.{privatehome, settings}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import org.slf4j.LoggerFactory

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.net.BindException
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object gui {
  private val logger = LoggerFactory.getLogger(this.getClass)
  implicit val actorSystem: ActorSystem = ActorSystem("system")
  implicit val exceptionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  var keystore: FileInputStream = _

  try {
    keystore = new FileInputStream(settings.keystore.path)
    ks.load(keystore, settings.keystore.password)
  } catch {
    case e: FileNotFoundException =>
      logger.error("Keystore not Found",e)
      privatehome.shutdown(78) //78 linux standard for config error or 74 linux standard for IO Error
    case e: IOException =>
      logger.error("Keystore passord wrong",e)
      privatehome.shutdown(78) //78 linux standard for config error even though it is a java IO exception it really is a config error because it gets thrown by the keystore decrypt because the password was wrong
    case e: Throwable =>
      logger.error("Unknown Error in Keystore setup",e)
      privatehome.shutdown(1) //I does not know what the error is
  }
  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, settings.keystore.password)
  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  tmf.init(ks)
  val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val websocketRoute: Route = {
    concat(
    path("test") {
      extractHost{ host =>
        redirect(s"https://$host:${settings.http.port}",StatusCodes.TemporaryRedirect)
      }
    },
    path(settings.websocket.path) {
      extractRequest { request =>
        handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
      }
    }
    )
  }

  val httpRoute: Route = {
    concat(
      pathEndOrSingleSlash {
        redirect("/Devices.html", StatusCodes.PermanentRedirect)
      },
      getFromResourceDirectory(settings.http.path)
    )
  }

  Http().newServerAt("0.0.0.0", settings.websocket.port).adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveMaxIdle(1.second))).enableHttps(https).bind(websocketRoute).failed.foreach(e => serverBindExeceptionhandler(e))
  Http().newServerAt("0.0.0.0", settings.http.port).enableHttps(https).bind(httpRoute).failed.foreach(e => serverBindExeceptionhandler(e))

  def serverBindExeceptionhandler(exception: Throwable): Unit = {
    exception.getCause match {
      case _: BindException =>
        privatehome.shutdown(75) // Linux Standard temp failure; user is invited to retry; most likely is another instance running or another server is listening to this port
      case e: Throwable =>
        logger.error("Unknown error in Webserver",e)
        privatehome.shutdown(1)
    }
  }

}

