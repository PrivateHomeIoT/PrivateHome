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

import PrivateHome.Devices.controlType.Slider
import PrivateHome.data
import org.slf4j.LoggerFactory

object stringCommandHandler {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def interpretMessage(msg: IPCCommand): IPCResponse = {
    try {
      /*
      val command: Array[String] = msg.stripSuffix(")").split('(')
      val args = if ((command.length == 2) && (command(1) != null)) command(1).split(',') else null
      logger.debug("command = {}", command(0))

      val uiCommand = command(0) match {
        case "commandAddUserBase64" =>
          uiControl.addUser(args(0), new String(Base64.getDecoder.decode(args(1))))
        case "commandRecreateDatabase" => uiControl.recreateDatabase()
        case "commandSafeCreateDatabase" => uiControl.safeCreateDatabase()
        case "getRandomId" => uiControl.randomNewId
        case "commandOn" => uiControl.on(args(0), args(1).toFloat)
        case "commandOff" => uiControl.off(args(0))
        case "commandAddDevice" => uiControl.addDevice(commandAddDevice(args(0), args(1), args(2), args(3), args(4), args(5), args(6).toBoolean, args(7).toInt, args(8)))
        case "commandAddController" => uiControl.addController(args(0))
        case "commandGetController" => uiControl.getController.map(tupel => s"${tupel._1}:${tupel._2}").mkString(",")
        case "getControllerKey" => data.getControllerMasterId(args(0)).keyArray.map(_ & 0xFF).mkString(",")  //"%02x" format _
        case "commandProgramController" =>
          uiControl.programController(args(0), args(1), args(2), args(3))

        case _ => logger.warn("Unknown Command")
      }
      uiCommand match {
        case c: Command => uiControl.receiveCommand(c)
        case s: String => s
        case _ => ""
      }


       */
      logger.debug("Received command: {}", msg)
      val answer: IPCResponse = msg match {
        case c: ipcAddControllerCommand => uiControl.addController(c.name)
          ipcSuccessResponse()
        case c: ipcAddDeviceCommand => uiControl.addDevice(commandAddDevice(c.id, c.switchType, c.name, c.systemCode, c.unitCode, c.controlType, c.keepState, c.pin, c.masterId))
          ipcSuccessResponse()
        case c: ipcAddUserCommand => uiControl.addUser(c.username, c.passHash)
          ipcSuccessResponse()
        case _: ipcGetControllerCommand => ipcGetControllerResponse(uiControl.getController)
        case c: ipcGetControllerKeyCommand => ipcGetControllerKeyResponse(data.getControllerMasterId(c.id).keyArray)
        case c: ipcGetDeviceCommand =>
          val switch = uiControl.getDevice(c.id)
          val switchData = ipcLongSwitchData(switch.id, switch.controlType == Slider, switch.name, switch.status, switch.switchtype)
          ipcGetDeviceResponse(switchData)
        case _: ipcGetDevicesCommand => val data = ipcGetDevicesResponse(uiControl.getDevices)
          println("Got list")
          data
        case _: ipcGetRandomId => ipcGetRandomIdResponse(uiControl.randomNewId)
        case c: ipcOffCommand => uiControl.off(c.id);
          ipcSuccessResponse()
        case c: ipcOnCommand => uiControl.on(c.id, c.percent);
          ipcSuccessResponse()
        case c: ipcProgramControllerCommand => uiControl.programController(c.masterId, c.path, c.ssid, c.pass);
          ipcSuccessResponse()
        case _: ipcRecreateDatabase => uiControl.recreateDatabase();
          ipcSuccessResponse()
        case _: ipcSafeCreateDatabase => uiControl.safeCreateDatabase();
          ipcSuccessResponse()
        case c: ipcUpdateDeviceCommand => uiControl.updateDevice(commandUpdateDevice(c.oldId, c.newId, c.keepState, c.name, c.controlType, c.switchType, c.systemCode, c.unitCode, c.pin, c.masterId));
          ipcSuccessResponse()
        case c => logger.error("IPCCommand with class {} not implemented in stringCommandHandler", msg.getClass.toString)
          ipcErrorResponse(c, new Exception("Command not Implemented"))
      }

      answer
    } catch {
      case e: Throwable => logger.error("Unknown Error while interpreting Console command", e)
        ipcErrorResponse(msg, e)
    }
  }
}
