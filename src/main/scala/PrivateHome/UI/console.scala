/*
 * PrivateHome
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

package PrivateHome.UI

import PrivateHome.Devices.controlType._
import PrivateHome.Devices.switchType._
import PrivateHome.{BuildInfo, UI}
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import org.scalasbt.ipcsocket.UnixDomainSocket

import java.io._
import java.nio.file.{Files, Path}
import scala.collection.immutable.ArraySeq
import scala.io.StdIn.readLine
import scala.reflect.ClassTag

object console {
  var StdInJava: Console = System.console()
  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands"),
    "addController" -> REPLCommand(_ => addController(), "Adds a new Controller that is needed for mqttSwitches."),
    "addUser" -> REPLCommand(_ => addUser(), "Adds a new User for the WebGui"),
    "addSwitch" -> REPLCommand(_ => addSwitch(false), "Adds a new Switch"),
    "addDimmer" -> REPLCommand(_ => addSwitch(true), "Adds a new dimmable Switch"),
    "dim" -> REPLCommand(_ => dim(), "Set the dimming value of a dimmable Switch"),
    "status" -> REPLCommand(_ => status(), "Show the status of Switches"),
    "recreateDatabase" -> REPLCommand(_ => recreate(), "Recreates the Database and deletes all data"),
    "safeCreate" -> REPLCommand(_ => safeCreate(), "Adds missing tables to the database"),
    "getKey" -> REPLCommand(_ => printControllerKey(), "Displays the Key of a given Controller."),
    "programController" -> REPLCommand(_ => programController(), "This will transfer the settings for a Controller")
  )
  var socket: UnixDomainSocket = _
  var triedConnecting = false
  var out: ObjectOutputStream = _
  var in: ObjectInputStream = _
  var interactive: Boolean = _
  var socketPath = s"/tmp/${BuildInfo.name}.sock"

  def recreate(): Unit = {
    val command = ipcRecreateDatabase()
    send(command)
  }

  def printControllerKey(): Unit = {
    val controllerId = getControllerId
    var tmpId: String = ""
    while (tmpId.isBlank) tmpId = send(ipcGetControllerKeyCommand(controllerId)).key.map(_ & 0xFF).mkString(",")
    println(tmpId)
  }

  def safeCreate(): Unit = send(new ipcSafeCreateDatabase)

  def main(args: Array[String]): Unit = {

    val arguments = new cliParser(this.getClass.getSimpleName, ArraySeq.unsafeWrapArray(args))
    try
      connect()
    catch {
      case e: Throwable => e.printStackTrace()
    }
    if (arguments.subcommand.isDefined) {
      interactive = arguments.interactive()

      arguments.subcommand.get match {
        case s: UI.status => status(s.id.getOrElse(""))
        case on: UI.on =>
          val id = getDeviceID(on.id.getOrElse(""))
          val percentFloat: Float = {
            if (on.percentage.isSupplied) on.percentage() / 100f
            else if (on.percentFloat.isSupplied) on.percentFloat()
            else 1f
          }
          send(ipcOnCommand(id, percentFloat))
        case off: UI.off => val id = getDeviceID(off.id())
          send(ipcOffCommand(id))
        case t: toggleSwitch =>
          val id = getDeviceID(t.id())
          val state: Float = if (send(ipcGetDeviceCommand(id)).device.status == 0) 1 else 0
          send(ipcOnCommand(id, state))
      }
    } else {
      interactive = true


      while (true) {
        val userInput = readLine("> ")

        if (commands.contains(userInput)) {
          commands(userInput).methodToCall.apply()
        } else {
          println("Unrecognized command. Please try again.")
        }
      }
    }
  }

  private def ensureConnection: Boolean = {
    if (!triedConnecting)
      throw new IllegalStateException("Not called connect before.")
    if (socket.isClosed) {
      println(("Connection is closed.\n" +
        "Reconnecting").stripMargin)
      connect()
    }
    true
  }

  def connect(): Unit = {
    socket = new UnixDomainSocket(socketPath)
    in = new ObjectInputStream(socket.getInputStream)
    out = new ObjectOutputStream(socket.getOutputStream)
    triedConnecting = true
  }

  def addSwitch(dimmable: Boolean): Unit = {
    val tmpId = send(ipcGetRandomId()).id
    var id = ""
    while (!id.matches("[-_a-zA-Z0-9]{5}")) {
      id = readLine(s"id[$tmpId]> ")
      if (id == "") id = tmpId
    }
    val name = readLine("name> ").replace(',', ' ')
    val keepStateChar = readLine("Keep State (y/n)> ")(0).toLower
    val keepState = keepStateChar == 'y' || keepStateChar == 'j'
    var switchType: switchType = null
    var systemCode: String = "null"
    var unitCode: String = "null"
    var chosenController: String = null
    var pin: Int = -1

    do {
      val switchTypeString = if (dimmable) "mqtt" else readLine("Control Type (mqtt/433mhz)> ").toLowerCase
      switchType = switchTypeString match {
        case "mqtt" =>
          chosenController = getControllerId
          while (!(pin > -1 && pin < 64)) {
            try pin = readLine("Pin number 0-63> ").toInt
            catch {
              case _: NumberFormatException =>
            }
          }

          MQTT
        case "433mhz" =>
          while (!systemCode.matches("[01]{5}")) systemCode = readLine("systemCode (00000 - 11111)> ")
          while (!unitCode.matches("[01]{5}")) unitCode = readLine("unitCode (00000 - 11111)> ")
          MHZ
        case _ => null
      }
    } while (switchType == null)

    send(ipcAddDeviceCommand(id, switchType, name, systemCode, unitCode, {
      if (dimmable) SLIDER else BUTTON
    }, keepState, pin, chosenController))

  }

  def readPassword(prompt: String): Array[Char] = {
    if (StdInJava == null) {
      println("Because System.console does not exist the password will get echoed.")
      readLine(prompt).toCharArray
    } else {
      StdInJava.readPassword(prompt)
    }
  }

  def programController(): Unit = {
    val standardPath = "/dev/ttyUSB0"
    val masterId = getControllerId
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
      } else if (ssid.isEmpty) ssid = ""
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


    send(ipcProgramControllerCommand(path, masterId, ssid, pass))
  }

  def addController(): Unit = {
    val name = readLine("Name> ")
    send(ipcAddControllerCommand(name))
  }

  def help(): Unit = {
    println("Available commands:\n")
    commands.toList.foreach(cmd => println(s"${cmd._1}:\t${cmd._2.description}"))
  }

  def addUser(): Unit = {
    console.StdInJava = StdInJava
    println("Please provide Username and Password (does not get echoed)")
    val username = readLine("Username> ")
    val passwd: Array[Char] = readPassword("Password> ")
    val argon2 = Argon2Factory.create(Argon2Types.ARGON2id)
    val passHash = argon2.hash(10, 64, 4, passwd)
    argon2.wipeArray(passwd)
    val command = ipcAddUserCommand(username, passHash)
    send(command)

  }

  def send(command: IPCCommand)(implicit m: ClassTag[command.response]): command.response = {
    ensureConnection
    out.writeObject(command)

    in.readObject() match {
      case r: ipcSuccessResponse
        if !r.success =>
        scala.Console.err.println(s"command: ${r.command} failed with exception:")
        r.exception.printStackTrace(scala.Console.err)
        throw new RuntimeException("Command failed")
      case response =>
        try {
          m.runtimeClass.cast(response).asInstanceOf[command.response]
        } catch {
          case e: ClassCastException => throw new InvalidClassException("Server did not response with expected class")
        }
    }
  }

  def getControllerId: String = {
    var chosenController: String = null
    val response = send(new ipcGetControllerCommand)
    val controller: Map[String, String] = response.controller

    println("Available Controller:")
    var counter = 0

    for (x <- controller) {
      println(s"$counter ${x._1} ${x._2} ")
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

  def dim(tmpId: String = ""): Unit = {
    val id = getDeviceID(tmpId)
    var run: Boolean = true
    while (run) {
      val percentString = readLine("Command (0.0...1)> ")
      try {
        val percent = percentString.toFloat
        if (percent == 0) {
          run = false
          send(ipcOffCommand(id))
        } else if (1 >= percent && percent > 0) {
          run = false
          send(ipcOnCommand(id, percent))
        }
      } catch {
        case _: NumberFormatException =>
      }
    }
  }


  def getDeviceID(tmpID: String = ""): String = {
    var id = tmpID
    val devices = send(ipcGetDevicesCommand())
    val deviceMap = devices.DeviceList
    val deviceIds = deviceMap.keys.to(List)
    var counter = 0
    if (!deviceMap.contains(id))
      if (interactive) {
        deviceMap.foreach(s => {
          println(s"$counter: id: ${s._1} name: ${s._2.name}")
          counter += 1
        })
        while (!deviceMap.contains(id)) {
          id = readLine("id/Index> ")
          try {
            id = deviceIds(id.toInt)
          } catch {
            case _: IndexOutOfBoundsException =>
            case _: NumberFormatException =>
          }
        }
      } else throw new IllegalArgumentException
    id
  }


  def status(tmpId: String = ""): Unit = {
    val id = getDeviceID(tmpId)
    val response = send(ipcGetDeviceCommand(id))
    println(response.device.status * 100)
  }

}


/**
 * A REPL Command holds a method that can be executed and a description.
 *
 * @param methodToCall the method to call, when the command is executed
 * @param description  the description to show when the user asks for help
 */
case class REPLCommand(methodToCall: Unit => Unit, description: String)