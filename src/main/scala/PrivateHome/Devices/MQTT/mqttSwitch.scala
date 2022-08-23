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

package PrivateHome.Devices.MQTT

import PrivateHome.Devices.{Switch, controlType, switchType}
import PrivateHome.Devices.switchType.MQTT
import PrivateHome.data
import org.slf4j.{LoggerFactory, MarkerFactory}

import scala.xml.Node

class mqttSwitch(_id: String, _keepStatus: Boolean, _name: String, setupControlType: controlType, private var _pin: Int, masterID: String = "") extends Switch(_id, _keepStatus, _name, setupControlType) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val commandMarker = MarkerFactory.getMarker("COMMAND")

  if (masterID != "") {
    controller = data.getControllerMasterId(masterID)
  }

  private var _controller: mqttController = _

  def controller: mqttController = _controller

  def controller_=(newController: mqttController): Unit = {
    if (_controller != newController) {
      if (newController.addSwitch(_pin, this)) {
        if (_controller != null)
          _controller.deleteSwitch(_pin)
        _controller = newController
      }
    }
  }

  def masterId: String = if (_controller != null) _controller.masterID else ""

  def pin: Int = _pin

  def changePinAndController(pPin: Int, masterId: String): Unit = {
    val newController = data.getControllerMasterId(masterId)
    if (_controller != null) {
      if (_controller.masterID == masterId) pin_=(pPin)
      else if (newController.addSwitch(pPin, this)) {
        _controller.deleteSwitch(_pin)

        _controller = newController
        _pin = pPin
      }
    }
  }

  def pin_=(pPin: Int): Unit = {
    if (pPin != _pin) {
      if (_controller == null) {
        _pin = pPin
      } else {
        if (_controller.changePin(_pin, pPin))
          _pin = pPin
        else {
          throw new IllegalArgumentException("pin is already used")
        }
      }
    }
  }

  /**
   * This method turns on the MQTT-Device.
   */
  def on(percent: Float): Unit = {
    logger.info(commandMarker, "Turning on Switch {} to {}%", name, percent * 100)
    _controller.sendCommand(_pin, percent)
  }

  /**
   * This method turns off the MQTT-Device.
   */
  def off(): Unit = {
    _controller.sendCommand(_pin, 0)
    logger.info(commandMarker, "Turning off Switch {}", name)
  }

  /**
   * This method generates from the attributes of this class an XML which you can save with editXML.scala
   *
   * @return It returns a Node which you can use in editXML.scala
   */
  override def toXml: Node = <switch id={id}>
    <type>MQTT</type>
    <id>
      {id}
    </id>
    <keepStatus>
      {_keepStatus}
    </keepStatus>
  </switch>

  def switchtype: switchType = MQTT
}

object mqttSwitch {
  def apply(_id: String, _keepStatus: Boolean, _name: String, setupControlType: controlType, _pin: Int, masterId: String = ""): mqttSwitch = new mqttSwitch(_id, _keepStatus, _name, setupControlType, _pin, masterId)
}