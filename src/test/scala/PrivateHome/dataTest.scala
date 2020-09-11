package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.Switch
import org.scalatest.funsuite.AnyFunSuite


class dataTest extends AnyFunSuite {

  settings.database.path = "mem:devices"

  settings.database.path = "mem:devices"


  test("testCreate") {
    data.create()
  }

  test("At Create empty") {
    val expected: Map[String, Switch] = Map()
    data.create()
    data.fillDevices()
    assertResult(expected)(data.devices)
  }

  test("Test: Add one Device") {
    data.create()
    data.addDevice(mqttSwitch("abcde", false, "Device one","button"))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", false, "Device one","button")))
    data.fillDevices()
    assertResult(expected)(data.devices)

  }

  test("Test: Add more Devices") {
    data.create()
    var expected: Map[String, Switch] = Map(("abcde", mqttSwitch("abcde", true, "Device two","button")), ("hkIKH", mhzSwitch("hkIKH", _keepStatus = false, "Device three", "10101", "01010")))
    val expected2: Map[String, Switch] = Map(("kijhe", mhzSwitch("kijhe", _keepStatus = false, "Device Four", "11111", "00000")), ("testd", mqttSwitch("testd", false, "Device five","slider")))
    expected.foreach(d => data.addDevice(d._2))

    data.fillDevices()
    assertResult(expected)(data.devices)
    expected2.foreach(d => data.addDevice(d._2))
    expected = expected.concat(expected2)
    data.fillDevices()
    assertResult(expected)(data.devices)

  }

  test("Save State") {
    data.create()
    data.addDevice(mqttSwitch("abcde", false, "Device","button"))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", false, "Device","button")))
    data.fillDevices()
  }

  test("Test: getDevice") {
    data.create()
    val switch = mqttSwitch("abcde", false, "Test","slider")
    data.addDevice(switch)
    assertResult(data.getDevice("abcde"))(switch)
  }


}