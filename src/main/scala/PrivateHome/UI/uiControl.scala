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

import PrivateHome.Devices.MQTT.mqttController
import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.data
import PrivateHome.data.{chars, devices}
import org.json4s.JsonAST.{JField, JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}
import org.slf4j.LoggerFactory

import java.lang.Integer.parseInt
import java.math.BigInteger
import java.security.SecureRandom

object uiControl {

  private val logger = LoggerFactory.getLogger(this.getClass)
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer

  /**
   * Turns on the specified switch
   * @param id The Id of the Switch
   * @param percent The percentage to what the switch should be turnt on (currently only supported for mqtt)
   */
  def on(id: String, percent: Float): Unit = {
    devices(id).on(percent)
  }

  /**
   * Turns off the specified switch
   * @param id The ID of the Switch
   */
  def off(id: String): Unit = {
    devices(id).off()
  }

  /**
   * Get a List of all Switches
   * @return A List containing all the switch objects that ar known to data
   */
  def getDevices: List[Switch] = {
    data.devices.values.toList
  }

  /**
   * Adds the switch defined by the commendAddDevice object
   * While doing this all needed data gets writen in the database and in all lookup tables/maps
   * @param command The object defining the Switch
   */
  def addDevice(command: commandAddDevice): Unit = {
    data.addDevice(Switch(command))
  }

  /**
   * Gets a specified switch object from the data object
   * @param id the Id of the Switch that you want
   * @return The Switch object used by data
   */
  def getDevice(id: String): Switch = {
    data.getDevice(id)
  }

  /**
   * Adds a user to the database thereby allowing them to login (currently you can not modify a User)
   * @param username The username for the new User
   * @param passHash The argon2id hashed password
   */
  def addUser(username: String, passHash: String): Unit = {
    data.addUser(username, passHash)
  }

  /**
   * This adds the currently missing databases but does not drop anything
   */
  def safeCreateDatabase(): Unit = {
    data.create()
  }

  /**
   * This drops all tables in the database and creates new ones.
   * This is currently needed if you want to upgrade the database as we don't provide upgrade scripts for SQL
   * @param dropTables This can override the dropping of the tables normally it is true but if you specify false it behaves like safeCreateDatabase
   */
  def recreateDatabase(dropTables: Boolean = true): Unit = {
    data.create(dropTables)
  }

  /**
   * Updates a switch according to the commandUpdateDevice object
   * @param command the commandUpdateDevice object specifying the old ID and all the new values
   */
  def updateDevice(command: commandUpdateDevice): Unit = {
    data.updateDevice(command.oldId, command)
  }

  /**
   * This generates a new ID that can be used for a new Switch because we check against the list of used ids.
   * @return the recommended ID for a new switch
   */
  def randomNewId: String = {

    var id: String = null
    val random = new SecureRandom()
    var run = true
    while (run) {

      id = new BigInteger(5 * 5, random).toString(32) //  This generates a random String with length 5
      try {
        data.idTest(id, create = true)
        logger.debug("Recomended id: \"{}\"", id)
        run = false
      }
      catch {
        case _: IllegalArgumentException =>
      }
    }
    id
  }

  /**
   * This writes the configuration for the controller running PrivateHomeIoT.ESPFirmware to the specified file.
   * @param masterId The masterID of the controller to program
   * @param path The path of the file to which the config should be writen. This file must exist
   * @param ssid The ssid that should be used tor the ESP
   * @param pass The password of the WiFi-Network
   */
  def programController(masterId: String, path: String, ssid: String, pass: String): Unit = {
    data.getControllerMasterId(masterId).programController(path, ssid, pass)
  }

  /**
   * Get a List of all the controllerIDs and their names
   * @return a simple List containing Tupels of (id, name)
   */
  def getController: List[(String, String)] = {
    data.masterIds.map(t => {
      t -> data.getControllerMasterId(t).name
    })
  }

  /**
   * Adds a new controller to the system this does not trigger a program controller and the ESP needs to be added after the fact.
   * @param name The name that should be used for this controller
   */
  def addController(name: String): Unit = {
    var masterId: String = ""
    do {
      masterId = ""
      new BigInteger(5 * 6, new SecureRandom()).toString(2).grouped(6).foreach(t => {
        masterId += chars(parseInt(t, 2) % 62)
      })
    } while (data.masterIdExists(masterId))
    val key: Array[Byte] = new BigInteger(16 * 8, new SecureRandom()).toByteArray.slice(0, 16)
    data.addController(new mqttController(masterId, key, name), key)
  }

  @deprecated("This was replaced by individual method's to have better return value handling","0.5.0")
  def receiveCommand(command: Command): Any = {
    try {
      command match {
        case c: commandOn => devices(c.id).on(c.percentFloat)
          true
        case c: commandOff => devices(c.id).off()
          true
        case _: commandGetDevices =>
          var devicesJson: List[JValue] = List()
          for (device <- data.devices) {

            devicesJson = devicesJson.concat(List(JsonMethods.parse(write(device._2))))
          }
          JObject(JField("devices", devicesJson)) // because we don't use the "~" we must lift it to JSON that is why we use JObject(JField()) instead an simple "devices" -> devicesJSon.
        case c: commandAddDevice => data.addDevice(Switch(c)); true
        case c: commandGetDevice => JsonMethods.parse(write(data.getDevice(c.id)))

        case c: commandAddUserBase64 => data.addUser(c.userName, c.passHash)
          true
        case c: commandRecreateDatabase => data.create(true); true
        case _: commandSafeCreateDatabase => data.create(); true
        case c: commandUpdateDevice => data.updateDevice(c.oldId, c); true
        case _: commandGetRandomId =>

          var id: String = null
          val random = new SecureRandom()
          var run = true
          while (run) {

            id = new BigInteger(5 * 5, random).toString(32) //  This generates a random String with length 5
            try {
              data.idTest(id, create = true)
              logger.debug("Recomended id: \"{}\"", id)
              run = false
            }
            catch {
              case _: IllegalArgumentException =>
            }
          }
          id
        case c: commandUpdateDevice => data.updateDevice(c.oldId, c); true
        case c: commandProgramController => data.getControllerMasterId(c.masterId).programController(c.path, c.ssid, c.pass)
        case _: commandGetController =>
          var answer = ""
          val masterids = data.masterIds
          val test: List[(String, String)] = masterids.map(t => {
            t -> data.getControllerMasterId(t).name
          })
          test
        case c: commandAddController =>
          var masterId: String = ""
          do {
            masterId = ""
            new BigInteger(5 * 6, new SecureRandom()).toString(2).grouped(6).foreach(t => {
              masterId += chars(parseInt(t, 2) % 62)
            })
          } while (data.masterIdExists(masterId))
          val key: Array[Byte] = new BigInteger(16 * 8, new SecureRandom()).toByteArray.slice(0, 16)
          data.addController(new mqttController(masterId, key, c.name), key)
      }
    } catch {
      case exception: Exception =>
        logger.debug("Unhandled exception will get forwarded to connector", exception)
        exception
    }
  }
}
