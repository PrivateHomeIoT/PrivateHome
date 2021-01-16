package PrivateHome

import PrivateHome.UI._
import akka.actor.ActorSystem
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types

import java.io.Console
import java.nio.file.{Path, Paths}
import java.util.Base64
import scala.concurrent.Future
import scala.io.StdIn.readLine

object console {

  implicit val actorSystem: ActorSystem = ActorSystem("system")
  val StdInJava: Console = System.console()
  val path: Path = Paths.get("/tmp/privatehome.sock") //here we need to use Java wich isn't that beautiful but outgoingConnection only accepts java.nio.file.Path
  val outConnection: Flow[ByteString, ByteString, Future[UnixDomainSocket.OutgoingConnection]] = UnixDomainSocket().outgoingConnection(path)
  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands"),
    "addUser" -> REPLCommand(_ => addUser(), "Adds a new User for the WebGui"),
    "addSwitch" -> REPLCommand(_ => addSwitch(false), "Adds a new Switch"),
    "addDimmer" -> REPLCommand(_ => addSwitch(true), "Adds a new dimmable Switch"),
    "dimm" -> REPLCommand(_ => dimm(), "Set the dimming value of a dimmable Switch"),
    //"status" -> REPLCommand(_ => status(), "Show the status of Switches"),
    "recreateDatabase" -> REPLCommand(_ => recreate(), "Recreates the Database and deletes all data")
  )

  def recreate(): Unit = {
    val command = new commandRecreateDatabase
    val s = Source.single(ByteString(command.toString + "\n")).via(outConnection).runWith(Sink.ignore)
  }

  def main(args: Array[String]): Unit = {

    while (true) {
      val userInput = readLine("> ")

      if (commands.contains(userInput)) {
        commands(userInput).methodToCall.apply()
      } else {
        println("Unrecognized command. Please try again.")
      }
    }
  }

  private def help(): Unit = {
    println("Available commands:\n")
    commands.foreach(cmd => println(s"${cmd._1}:\t${cmd._2.description}"))
  }

  private def addUser(): Unit = {
    println("Please provide Username and Password (does not get echoed)")
    val username = readLine("Username> ")
    var passwd: Array[Char] = null
    if (StdInJava != null) {
      passwd = StdInJava.readPassword("Password> ")
    }
    else {
      println("Because the System.console() object does not exist we use a simple readLine so the password will be echoed")
      passwd = readLine("Password> ").toCharArray
    }
    val argon2 = Argon2Factory.create(Argon2Types.ARGON2id)
    val passHash = Base64.getEncoder.encodeToString(argon2.hash(10, 64, 4, passwd).getBytes)
    val command = commandAddUserBase64(username, passHash)
    send(command)

  }

  private def send(command: Command): Unit = {
    send(command.toString)
  }

  private def addSwitch(dimmable: Boolean): Unit = {
    val result = Source.single(ByteString("getRandomId")).via(outConnection).runWith(Sink.fold(ByteString("")) { case (acc, b) => acc ++ b })
    while (!result.isCompleted) {}
    result.value.get.get.utf8String
    var id: String = ""
    while (!id.matches("[-_a-zA-Z0-9]{5}")) id = readLine("id> ")
    val name = readLine("name> ").replace(',', ' ')
    val keepStateChar = readLine("Keep State (y/n)")(0).toLower
    val keepState = keepStateChar == 'y' || keepStateChar == 'j'
    var switchType: String = null
    var systemCode: String = "null"
    var unitCode: String = "null"
    if (dimmable) switchType = "mqtt"
    else {
      while (switchType == null) {
        switchType = readLine("Control Type (mqtt/433mhz)> ").toLowerCase
        switchType = switchType match {
          case "mqtt" => "mqtt"
          case "433mhz" =>
            while (!systemCode.matches("[01]{5}")) systemCode = readLine("systemCode (00000 - 11111)> ")
            while (!unitCode.matches("[01]{5}")) unitCode = readLine("unitCode (00000 - 11111)> ")
            "433Mhz"
          case _ => null
        }
      }
    }
    send(s"commandAddDevice($id,$switchType,$name,$systemCode,$unitCode,${if (dimmable) "slider" else "button"},$keepState)")

  }

  private def dimm(): Unit = {
    var id: String = ""
    while (!id.matches("[-_a-zA-Z0-9]{5}")) id = readLine("id> ")
    var run: Boolean = true
    while (run) {
      val percentString = readLine("Command (0.0...1)> ")
      try {
        if (percentString.toFloat == 0) {
          send(s"commandOff($id)")
          run = false
        } else if (1 >= percentString.toFloat && percentString.toFloat > 0) {
          send(s"commandOn($id,$percentString)")
          run = false
        }
      } catch {
        case _: Throwable =>
      }

    }
  }

  private def send(msg: String): Unit = {
    Source.single(ByteString(msg + "\n")).via(outConnection).runWith(Sink.ignore)
  }

  private def status(): Unit = {

  }

}

/**
 * A REPL Command holds a method that can be executed and a description.
 *
 * @param methodToCall the method to call, when the command is executed
 * @param description  the description to show when the user asks for help
 */
case class REPLCommand(methodToCall: Unit => Unit, description: String)