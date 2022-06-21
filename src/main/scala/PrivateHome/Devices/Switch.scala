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

package PrivateHome.Devices

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.controlType.{BUTTON, controlType}
import PrivateHome.Devices.switchType.{MHZ, MQTT}
import PrivateHome.UI.Websocket.websocket
import PrivateHome.UI.{commandAddDevice, commandUpdateDevice}
import PrivateHome.data
import PrivateHome.data.idTest
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, JsonAST}

import scala.xml._

/**
 * The general Switch class
 *
 * @param setupID    an unique Base64 ID
 * @param keepStatus toggles if the Switch should save State over program restart (failure)
 */

abstract class Switch(private var setupID: String, var keepStatus: Boolean, var name: String, private var _controlType: controlType) {
  idTest(setupID)

  private var _status: Float = 0

  def on(percent: Float): Unit

  def off(): Unit

  def switchtype: switchType.switchType

  @deprecated("Will be removed with editXML", "0.3.1")
  def toXml: Node

  def serializer: JsonAST.JObject = {
    ("id" -> id) ~ ("keepState" -> keepStatus) ~ ("name" -> name) ~ ("switchType" -> switchtype.toString) ~ ("controlType" -> controlType.toString) ~ ("status" -> status)
  }

  def id: String = setupID

  def id_=(pId: String): Unit = {
    if (pId != setupID) {
      idTest(pId, create = true)
      setupID = pId
    }
  }

  def controlType: controlType = _controlType

  def controlType_=(newType: controlType): Unit = {
    _controlType = newType
  }

  def status: Float = _status

  /**
   * Sets the Status of the Switch only changes State after receiving a Confirmation
   *
   * @param state The State the Switch should change to
   */
  def status_=(state: Float): Unit = {
    _status = state
    if (keepStatus) data.saveStatus(id, state)
    websocket.broadcastMsg(("Command" -> "statusChange") ~ ("answer" -> (("id" -> id) ~ ("status" -> state) ~ ("type" -> _controlType.toString))))
  }

}

object Switch {
  def apply(data: commandAddDevice): Switch = {
    data.switchType match {
      case MHZ => mhzSwitch(data.id, data.keepState, data.name, data.systemCode, data.unitCode)
      case MQTT => mqttSwitch(data.id, data.keepState, data.name, data.controlType, data.pin, data.masterId)
    }
  }

  def apply(data: commandUpdateDevice): Switch = {
    data.switchType match {
      case MHZ => mhzSwitch(data.newId, data.keepState, data.name, data.systemCode, data.unitCode)
      case MQTT => mqttSwitch(data.newId, data.keepState, data.name, data.controlType, data.pin, data.masterId)
    }
  }

  def on(percent: Float, id: String): Unit = {
    val tempSwitch = data.devices(id)
    tempSwitch.on(percent)
  }

  def off(id: String): Unit = {
    val tempSwitch = data.devices(id)
    tempSwitch.off()
  }
}

class switchSerializer extends CustomSerializer[Switch](ser = format => ( {
  case jsonObj: JObject => mqttSwitch("", _keepStatus = false, "This Switch should never be used", BUTTON, 0)
}, {
  case switch: mqttSwitch => switch.serializer ~ ("pin" -> switch.pin) ~ ("masterId" -> switch.masterId)
  case switch: mhzSwitch => switch.serializer ~ ("systemCode" -> switch.systemCode) ~ ("unitCode" -> switch.unitCode)
}))

object switchType extends Enumeration {
  type switchType = Value
  val MHZ, MQTT = Value
}

object controlType extends Enumeration {
  type controlType = Value
  val BUTTON, SLIDER = Value
}
