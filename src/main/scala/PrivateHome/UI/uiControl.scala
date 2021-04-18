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

import PrivateHome.Devices.{Switch, switchSerializer}
import PrivateHome.data
import PrivateHome.data.devices
import org.json4s.JsonAST.{JField, JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}
import org.slf4j.LoggerFactory

import java.math.BigInteger
import java.security.SecureRandom

object uiControl {

  implicit val formats: Formats = Serialization.formats(NoTypeHints) + new switchSerializer
  private val logger = LoggerFactory.getLogger(this.getClass)

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
        case c: commandUpdateDevice => data.updateDevice(c.oldId,Switch(c)); true
        case _: commandGetRandomId =>

          var id: String = null
          val random = new SecureRandom()
          var run = true
          while (run) {

            id = new BigInteger(5 * 5, random).toString(32) //  This generates a random String with length 5
            try {
              data.idTest(id, create = true)
              logger.debug("Recomended id: \"{}\"",id)
              run = false
            }
            catch {
              case _: IllegalArgumentException =>
            }
          }
          id
      }
    } catch {
      case exception: Exception => exception
    }
  }
}
