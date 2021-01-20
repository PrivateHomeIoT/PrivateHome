package PrivateHome.UI.GUI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.settings
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.net.BindException
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object gui {
  implicit val actorSystem: ActorSystem = ActorSystem("system")
  implicit val exectionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  var keystore: FileInputStream = _

  try {
    keystore = new FileInputStream(settings.keystore.path)
    ks.load(keystore, settings.keystore.pass.toCharArray)
  } catch {
    case e: FileNotFoundException =>
      Console.err.println(e)
      e.printStackTrace()
      sys.exit(78) //78 linux standard for config error or 74 linux standard for IO Error
    case e: IOException =>
      Console.err.println(Console.RED + e)
      sys.exit(78) //78 linux standard for config error even though it is a java IO exception it really is a config error because it gets thrown by the keystore decrypt because the password was wrong
    case e: Throwable =>
      println(e)
      e.printStackTrace(Console.err)
      sys.exit(1) //I does not know what the error is
  }
  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, settings.keystore.pass.toCharArray)
  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  tmf.init(ks)
  val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val websocketRoute: Route = path(settings.websocket.path) {
    extractRequest { request =>
      handleWebSocketMessages(websocket.listen(request.getHeader("Sec-WebSocket-Key").get().value()))
    }
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
      case e: BindException =>
        Console.err.println(Console.RED + e.getMessage.split("\n")(0))
        sys.exit(75) // Linux Standard temp failure; user is invited to retry; most likely is another instance running or another server is listening to this port
      case e: Throwable =>
        e.printStackTrace(Console.err)
        sys.exit(1)
    }
  }

}

