package PrivateHome

import PrivateHome.Devices.MQTT.mqttSwitch
import org.scalatest.FunSuite

class dataSQLTest extends FunSuite {



  test("testGetDevices") {

  }

  test("testCreate") {
    assert(dataSQL.create())
  }

  test("testAddDevice") {
    dataSQL.create()
    dataSQL.addDevice(mqttSwitch("abcde",keepStatus = true))
    assertResult(Seq[Dev])(dataSQL.devices)

  }

}
