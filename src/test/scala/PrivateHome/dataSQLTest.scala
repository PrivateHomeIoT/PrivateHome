package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import PrivateHome.Devices.MQTT.mqttSwitch
import PrivateHome.Devices.Switch
import org.scalatest.FunSuite


class dataSQLTest extends FunSuite {


  test("testCreate") {
    dataSQL.create()
  }

  test("At Create empty") {
    val expected: Map[String, Switch] = Map()
    dataSQL.create()
    dataSQL.fillDevices()
    assertResult(expected)(dataSQL.devices)
  }

  test("Test: Add one Device") {
    dataSQL.create()
    dataSQL.addDevice(mqttSwitch("abcde", keepStatus = false))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", keepStatus = false)))
    dataSQL.fillDevices()
    assertResult(expected)(dataSQL.devices)

  }

  test("Test: Add more Devices") {
    dataSQL.create()
    var expected: Map[String, Switch] = Map(("abcde", mqttSwitch("abcde", keepStatus = true)),("hkIKH",mhzSwitch("hkIKH",_keepStatus = false,"10101","01010")))
    val expected2: Map[String, Switch] = Map(("kijhe",mhzSwitch("kijhe",_keepStatus = false,"11111","00000")),("testd",mqttSwitch("testd",keepStatus = false)))
    expected.foreach(d => dataSQL.addDevice(d._2))

    dataSQL.fillDevices()
    assertResult(expected)(dataSQL.devices)
    expected2.foreach(d => dataSQL.addDevice(d._2))
    expected = expected.concat(expected2)
    dataSQL.fillDevices()
    assertResult(expected)(dataSQL.devices)

  }

  test("Save State") {
    dataSQL.create()
    dataSQL.addDevice(mqttSwitch("abcde", keepStatus = false))
    var expected: Map[String, Switch] = Map()
    expected = expected + (("abcde", mqttSwitch("abcde", keepStatus = false)))
    dataSQL.fillDevices()
  }

  test("Test: getDevice") {
    dataSQL.create()
    var switch = mqttSwitch("abcde", keepStatus = false)
    dataSQL.addDevice(switch)
    assertResult(dataSQL.getDevice("abcde"))(switch)
  }



}