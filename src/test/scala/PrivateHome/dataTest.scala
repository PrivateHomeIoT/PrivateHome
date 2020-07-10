package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.Switch
import org.scalatest.FunSuite


class dataTest extends FunSuite {


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
    data.addDevice(mqttSwitch("abcde", keepStatus = false))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", keepStatus = false)))
    data.fillDevices()
    assertResult(expected)(data.devices)

  }

  test("Test: Add more Devices") {
    data.create()
    var expected: Map[String, Switch] = Map(("abcde", mqttSwitch("abcde", keepStatus = true)),("hkIKH",mhzSwitch("hkIKH",_keepStatus = false,"10101","01010")))
    val expected2: Map[String, Switch] = Map(("kijhe",mhzSwitch("kijhe",_keepStatus = false,"11111","00000")),("testd",mqttSwitch("testd",keepStatus = false)))
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
    data.addDevice(mqttSwitch("abcde", keepStatus = false))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", keepStatus = false)))
    data.fillDevices()
  }

  test("Test: getDevice") {
    data.create()
    var switch = mqttSwitch("abcde", keepStatus = false)
    data.addDevice(switch)
    assertResult(data.getDevice("abcde"))(switch)
  }



}