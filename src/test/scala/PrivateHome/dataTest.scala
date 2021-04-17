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

import PrivateHome.Devices.MQTT.{mqttController, mqttSwitch}
import PrivateHome.Devices.Switch
import org.scalatest.funsuite.AnyFunSuite


class dataTest extends AnyFunSuite {
  settings.database.path = "mem:devices"
  val key = Array[Byte](0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)


  test("testCreate") {
    data.create(true)
  }

  test("At Create empty") {
    val expected: Map[String, Switch] = Map()
    data.create(true)
    data.fillDevices()
    assertResult(expected)(data.devices)
  }

  test("Test: Add one Device") {
    data.create(true)
    data.addController(new mqttController("aaaaa",key),key)
    data.addDevice(mqttSwitch("abcde", _keepStatus = false, "Device one", "button",1,"aaaaa"))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", _keepStatus = false, "Device one", "button",1,"aaaaa")))
    data.fillDevices()
    assertResult(expected)(data.devices)

  }

  test("Test: Add more Devices") {
    data.create(true)
    data.addController(new mqttController("aaaaa",key),key)
    var expected: Map[String, Switch] = Map(("abcde", mqttSwitch("abcde", _keepStatus = true, "Device two", "button",1,"aaaaa")), ("hkIKH", mhzSwitch("hkIKH", _keepStatus = false, "Device three", "10101", "01010")))
    val expected2: Map[String, Switch] = Map(("kijhe", mhzSwitch("kijhe", _keepStatus = false, "Device Four", "11111", "00000")), ("testd", mqttSwitch("testd", _keepStatus = false, "Device five", "slider",2,"aaaaa")))
    expected.foreach(d => data.addDevice(d._2))

    data.fillDevices()
    assertResult(expected)(data.devices)
    expected2.foreach(d => data.addDevice(d._2))
    expected = expected.concat(expected2)
    data.fillDevices()
    assertResult(expected)(data.devices)

  }

  test("Save State") {
    data.create(true)
    data.addController(new mqttController("aaaaa",key),key)
    data.addDevice(mqttSwitch("abcde", _keepStatus = false, "Device", "button",1,"aaaaa"))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", _keepStatus = false, "Device", "button",1,"aaaaa")))
    data.fillDevices()
  }

  test("Test: getDevice") {
    data.create(true)
    data.addController(new mqttController("aaaaa",key),key)
    val switch = mqttSwitch("abcde", _keepStatus = false, "Test", "slider",1,"aaaaa")
    data.addDevice(switch)
    assertResult(data.getDevice("abcde"))(switch)
  }


}
