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

package PrivateHome.Devices.MHz

import PrivateHome.Devices.Switch
import PrivateHome.Devices.controlType.BUTTON
import PrivateHome.Devices.switchType._
import org.slf4j.{LoggerFactory, MarkerFactory}

import scala.xml.Node

/**
 * An Switch class that uses 433MHz to communicate to the Switch
 *
 * @param setupID     an unique Base64 ID
 * @param _keepStatus toggles if the Switch should save State over program restart (failure)
 * @param _systemCode The SystemCode used for the Switch
 * @param _unitCode   The UnitCode used for the Switch
 */
case class mhzSwitch(setupID: String, _keepStatus: Boolean, _name: String, private var _systemCode: String, private var _unitCode: String) extends Switch(setupID, _keepStatus, _name, BUTTON) {
  if (_systemCode.length != 5) throw new IllegalArgumentException(s"System code ${_systemCode} should be 5 Char long")
  if (_unitCode.length != 5) throw new IllegalArgumentException(s"Unit Code ${_unitCode} should be 5 Char long")

  if (!_systemCode.matches("[01]{5}")) throw new IllegalArgumentException(s"System code ${_systemCode} should only contain 0/1")
  if (!_unitCode.matches("[01]{5}")) throw new IllegalArgumentException(s"Unit code ${_unitCode} should only contain 0/1")

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val commandMarker = MarkerFactory.getMarker("COMMAND")

  /**
   * Changes the SystemCode
   *
   * @param newSystemCode The new System Code
   */
  def systemCode_=(newSystemCode: String): Unit = {
    if (_systemCode.length != 5) throw new IllegalArgumentException(s"System code ${_systemCode} should be 5 Char long")
    if (!_systemCode.matches("[01]{5}")) throw new IllegalArgumentException(s"System code ${_systemCode} should only contain 0/1")
    _systemCode = newSystemCode
  }

  /**
   * Changes the Unit Code
   *
   * @param newUnitCode The new Unit Code
   */
  def unitCode_=(newUnitCode: String): Unit = {
    if (_unitCode.length != 5) throw new IllegalArgumentException(s"Unit Code ${_unitCode} should be 5 Char long")
    if (!_unitCode.matches("[01]{5}")) throw new IllegalArgumentException(s"Unit code ${_unitCode} should only contain 0/1")
    _unitCode = newUnitCode
  }

  /**
   * Turns the Switch on
   */
  override def on(percent:  Float): Unit = {
    logger.info(commandMarker, "Turning on Switch {} to {}%", name, 100)
    sendMhz(mhzCommand(_systemCode, _unitCode, command = true))
  }

  /**
   * Turns the Switch off
   */
  override def off(): Unit = {
    logger.info(commandMarker, "Turning off Switch {}", name)
    sendMhz(mhzCommand(_systemCode, _unitCode, command = false))
  }

  /**
   * Generates a XML Node containing all Information to Regenerate the object
   *
   * @return The Node Object
   */
  override def toXml: Node = <switch id={id}>
    <type>433MHz</type>
    <keepStatus>{_keepStatus}</keepStatus>
    <systemCode>{systemCode}</systemCode>
    <unitCode>{unitCode}</unitCode>
  </switch>

  /**
   *
   * @return The System Code
   */
  def systemCode: String = _systemCode

  override def switchtype = MHZ

  /**
   *
   * @return The Unit Code
   */
  def unitCode: String = _unitCode
}
