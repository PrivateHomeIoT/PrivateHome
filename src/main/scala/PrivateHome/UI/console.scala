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

package PrivateHome.UI

import PrivateHome.Devices.controlType._
import PrivateHome.Devices.switchType._
import PrivateHome.{BuildInfo, UI}
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import org.scalasbt.ipcsocket.UnixDomainSocket

import java.io._
import java.nio.file.{Files, Path}
import scala.io.StdIn.readLine

object console {
  val StdInJava: Console = System.console()
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
    while (in.available() > 0) println(in.readObject())
    send(ipcGetControllerKeyCommand(controllerId))
    var tmpId: String = ""
    while (tmpId.isBlank) tmpId = read[ipcGetControllerKeyResponse]().key.map(_ & 0xFF).mkString(",")
    println(tmpId)
  }

  def safeCreate(): Unit = send(new ipcSafeCreateDatabase)

  def main(args: Array[String]): Unit = {

    val arguments = new cliParser(this.getClass.getSimpleName, args)
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
          send(ipcGetDeviceCommand(id))
          val state: Float = if (read[ipcGetDeviceResponse]().device.status == 0) 1 else 0
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

  def help(): Unit = {
    println("Available commands:\n")
    commands.toList.foreach(cmd => println(s"${cmd._1}:\t${cmd._2.description}"))
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
    val passHash = argon2.hash(10, 64, 4, passwd)
    val command = ipcAddUserCommand(username, passHash)
    send(command)

  }


  private def addSwitch(dimmable: Boolean): Unit = {
    send(ipcGetRandomId())
    val response = read[ipcGetRandomIdResponse]()
    val tmpId = response.id
    var id = ""
    while (!id.matches("[-_a-zA-Z0-9]{5}")) {
      id = readLine(s"id[$tmpId]> ")
      if (id == "") id = tmpId
    }
    val name = readLine("name> ").replace(',', ' ')
    val keepStateChar = readLine("Keep State (y/n)")(0).toLower
    val keepState = keepStateChar == 'y' || keepStateChar == 'j'
    var switchType: switchType = null
    var systemCode: String = "null"
    var unitCode: String = "null"
    var chosenController: String = null
    var pin = -1

    do {
      val switchTypeString = if (dimmable) MQTT else readLine("Control Type (mqtt/433mhz)> ").toLowerCase
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

  private def dim(tmpId: String = ""): Unit = {
    val id = getDeviceID(tmpId)
    var run: Boolean = true
    while (run) {
      val percentString = readLine("Command (0.0...1)> ")
      try {
        val percent = percentString.toFloat
        if (percent == 0) {
          send(ipcOffCommand(id))
          run = false
        } else if (1 >= percent && percent > 0) {
          send(ipcOnCommand(id, percent))
          run = false
        }
      } catch {
        case _: Throwable =>
      }
      read[ipcSuccessResponse]()

    }
  }

  private def status(tmpId: String = ""): Unit = {
    val id = getDeviceID(tmpId)
    send(ipcGetDeviceCommand(id))
    val response = read[ipcGetDeviceResponse]()
    println(response.device.status * 100)
  }

  private def getDeviceID(tmpID: String = ""): String = {
    var id = tmpID
    send(ipcGetDevicesCommand())
    val devices = read[ipcGetDevicesResponse]()
    val deviceMap = devices.DeviceList
    val deviceIds = deviceMap.keys.to(List)
    var counter = 0
    if (interactive) {
      if (!deviceMap.contains(id))
        deviceMap.foreach(s => {
          println(s"$counter: id: ${s._1} name: ${s._2.name}")
          counter += 1
        })
      while (!deviceMap.contains(id)) {
        id = readLine("id/Index> ")
        try {
          id = deviceIds(id.toInt)
        } catch {
          case _: ArrayIndexOutOfBoundsException =>
          case _: NumberFormatException =>
        }
      }
    } else if (!deviceMap.contains(id)) throw new IllegalArgumentException
    id
  }

}


/**
 * A REPL Command holds a method that can be executed and a description.
 *
 * @param methodToCall the method to call, when the command is executed
 * @param description  the description to show when the user asks for help
 */
case class REPLCommand(methodToCall: Unit => Unit, description: String)