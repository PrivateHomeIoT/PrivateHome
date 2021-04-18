/*
 * Privatehome
 *     Copyright (C) 2021  RaHoni honisuess@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package PrivateHome

import PrivateHome.UI._
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import org.scalasbt.ipcsocket.UnixDomainSocket

import java.io.{BufferedReader, Console, InputStreamReader, PrintWriter}
import java.nio.file.{Files, Path}
import java.util.Base64
import scala.io.StdIn.readLine

object console {
  val StdInJava: Console = System.console()
  val socket = new UnixDomainSocket("/tmp/privatehome2.sock")
  val out = new PrintWriter(socket.getOutputStream)
  val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands"),
    "addUser" -> REPLCommand(_ => addUser(), "Adds a new User for the WebGui"),
    "addSwitch" -> REPLCommand(_ => addSwitch(false), "Adds a new Switch"),
    "addDimmer" -> REPLCommand(_ => addSwitch(true), "Adds a new dimmable Switch"),
    "dimm" -> REPLCommand(_ => dimm(), "Set the dimming value of a dimmable Switch"),
    //"status" -> REPLCommand(_ => status(), "Show the status of Switches"),
    "recreateDatabase" -> REPLCommand(_ => recreate(), "Recreates the Database and deletes all data"),
    "safeCreate" -> REPLCommand(_ => safeCreate(), "Adds missing tables to the database"),
    "addController" -> REPLCommand(_ => addController(), "Adds a new Controller that is needed for mqttSwitches."),
    "getKey" -> REPLCommand(_ => getControllerKey(), "Displays the Key of a given Controller."),
    "programController" -> REPLCommand(_ => programController(), "This will transfer the settings for a Controller")
  )

  def recreate(): Unit = {
    val command = new commandRecreateDatabase
    send(command)
  }

  def getControllerKey(): Unit = {
    val controllerId = getControllerId()
    while (in.ready()) println(in.readLine())
    send(s"getControllerKey($controllerId)")
    val tmpId: String = in.readLine()
    while (in.ready()) println(in.readLine())
    println(tmpId)
  }

  def safeCreate(): Unit = send(new commandSafeCreateDatabase)

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
    while (in.ready()) println(in.readLine())
    send("getRandomId")
    val tmpId: String = in.readLine()
    while (in.ready()) println(in.readLine())
    var id = ""
    while (!id.matches("[-_a-zA-Z0-9]{5}")) {
      id = readLine(s"id[$tmpId]> ")
      if (id == "") id = tmpId
    }
    val name = readLine("name> ").replace(',', ' ')
    val keepStateChar = readLine("Keep State (y/n)")(0).toLower
    val keepState = keepStateChar == 'y' || keepStateChar == 'j'
    var switchType: String = null
    var systemCode: String = "null"
    var unitCode: String = "null"
    var chosenController: String = null
    var pin = -1

      do {
        switchType = if (dimmable) "mqtt" else readLine("Control Type (mqtt/433mhz)> ").toLowerCase
        switchType = switchType match {
          case "mqtt" =>
            chosenController = getControllerId()
            while (!(pin > -1 && pin < 64)) {
              try pin = readLine("Pin number 0-63> ").toInt
              catch {
                case _: NumberFormatException =>
              }
            }

            "mqtt"
          case "433mhz" =>
            while (!systemCode.matches("[01]{5}")) systemCode = readLine("systemCode (00000 - 11111)> ")
            while (!unitCode.matches("[01]{5}")) unitCode = readLine("unitCode (00000 - 11111)> ")
            "433Mhz"
          case _ => null
        }
      } while (switchType == null)

    send(s"commandAddDevice($id,$switchType,$name,$systemCode,$unitCode,${if (dimmable) "slider" else "button"},$keepState,$pin,$chosenController)")

  }

  private def getControllerId(): String = {
    var chosenController: String = null
    while (in.ready()) println(in.readLine())
    send(new commandGetController)
    val answer: String = in.readLine()
    while (in.ready()) println(in.readLine())
    var controller: Map[String, String] = Map[String, String]()
    answer.split(",").foreach(tmp => {
      val controllerIdName = tmp.split(":")
      if (controllerIdName.length == 1)
        println(answer)
      controller += ((controllerIdName(0), controllerIdName(1)))
    })
    println("Available Controller:")
    var counter = 0

    for (x <- controller) {
      print(s"$counter ${x._1} ${x._2} ")
      counter += 1
    }
    println()

    while (chosenController == null) {
      chosenController = readLine("Chose Controller by number or ID \n> ")
      try {
        chosenController = controller.keys.to(Array)(chosenController.toInt)
      } catch {
        case _: ArrayIndexOutOfBoundsException =>
        case _: NumberFormatException =>
      }
      if (!controller.contains(chosenController)) chosenController = null
    }
    chosenController
  }

  def programController(): Unit = {
    val standardPath = "/dev/ttyUSB0"
    val masterId = getControllerId()
    var ssid = ""
    var pass = ""
    var path = ""
    var loop = true
    println("Next you can enter your SSID/WiFi name and Wifi Password if you just press enter they won't get set")
    while (loop) {
      loop = false
      ssid = readLine("ssid >")
      if (ssid.length > 32) {
        println("SSID has a max length of 32")
        loop = true
      }else if (ssid.isEmpty) ssid = ""
    }

    loop = true
    while (loop) {
      loop = false
      pass = readLine("pass >")
      if (pass.length > 64) {
        println("Password has a max length of 64")
        loop = true
      } else if (pass.isEmpty) pass = ""
    }

    while (path.isBlank) {
      path = readLine("interface path [%s]>", standardPath)
      if (path.isBlank) path = standardPath
      else {
        if (!Files.isRegularFile(Path.of(path))) {
          println("Path is not a File")
          path = ""
        }
      }
    }


    send(commandProgramController(path, masterId, ssid, pass))
  }

  def addController(): Unit = {
    val name = readLine("Name> ")
    send(commandAddController(name))
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
    out.println(msg)
    out.flush()
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