package PrivateHome.UI

import PrivateHome.UI.Websocket.websocket
import PrivateHome.data.devices
import org.json4s.JsonAST
import org.json4s.JsonDSL._

object uiControl {
  def receiveCommand(command: Command): Unit = {
    command match {
      case c: commandOn => devices(c.id).on(c.percent)
      case c: commandOff => devices(c.id).off()
      case c: commandGetDevices => {
        val devicesJson: List[JsonAST.JObject] = List()
        for (device <- devices) {
          devicesJson.concat(List(("id" -> device._2.id) ~ ("status" -> device._2.Status)))
        }
        websocket.sendMsg("devices" -> devicesJson)
      }
    }

  }
}
