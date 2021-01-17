package PrivateHome.UI

import PrivateHome.UI.GUI.gui.actorSystem
import PrivateHome.data
import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.{SourceQueueWithComplete, _}
import akka.util.ByteString

import java.math.BigInteger
import java.nio.file.{Path, Paths}
import java.security.SecureRandom
import scala.concurrent.Future


class repl {
  val path: Path = Paths.get("/tmp/privatehome.sock") //here we need to use Java wich isn't that beautiful but bind and handle only accepts java.nio.file.Path
  val binding: Future[UnixDomainSocket.ServerBinding] = UnixDomainSocket().bindAndHandle(cliHandler.listen(), path)
}

object cliHandler {
  def listen(): Flow[ByteString, ByteString, NotUsed] = {
    var connectionOut: ByteString => Unit = null
    var args: Array[String] = null
    val inboundHandle: Sink[String, Any] = Sink.foreach(msg => {
      try {
        val command: Array[String] = msg.stripSuffix(")").split('(')
        args = if ((command.length == 2) && (command(1) != null)) command(1).split(',') else null
        println(s"command = ${command(0)}")

        val uiCommand: Command = command(0) match {
          case "commandAddUserBase64" =>
            commandAddUserBase64(args(0), args(1))
          case "commandRecreateDatabase" => commandRecreateDatabase()
          case "getRandomId" =>
            val random = new SecureRandom()
            var id: String = null
            var run = true
            while (run) {
              id = new BigInteger(5 * 5, random).toString(32) //  This generates a random String with length 5
              try {
                data.idTest(id, create = true)
                println(id)
                connectionOut.apply(ByteString(id + "\n"))
                run = false
              }
              catch {
                case _: IllegalArgumentException =>
              }
            }
            new Command
          case "commandOn" => commandOn(args(0), args(1))
          case "commandOff" => commandOff(args(0))
          case "commandAddDevice" => commandAddDevice(args(0), args(1), args(2), args(3), args(4), args(5), args(6).toBoolean)

          case _ => println("Unknown Command")
            new Command
        }
        uiControl.receiveCommand(uiCommand)

      } catch {
        case e: Throwable => println(e)
          e.printStackTrace()
      }
    })
    val inbound: Sink[ByteString, NotUsed] = Flow[ByteString].via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true)).map(_.utf8String).to(inboundHandle)

    val outbound: Source[ByteString, SourceQueueWithComplete[ByteString]] = Source.queue[ByteString](16, OverflowStrategy.fail)
    Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {
      connectionOut = outboundMat.offer
      NotUsed
    })

  }
}