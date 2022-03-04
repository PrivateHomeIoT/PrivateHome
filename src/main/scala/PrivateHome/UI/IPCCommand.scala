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

import PrivateHome.Devices.controlType.controlType
import PrivateHome.Devices.switchType._

import java.util.Base64

trait IPCCommand extends Serializable {
  protected def testArgumentsSwitch(switchType: switchType, masterId: String, pin: Int, systemCode: String, unitCode: String): Boolean = {
    switchType match {
      case MQTT =>
        if (masterId.length != 5) throw new IllegalArgumentException("""Length of masterId is not 5""")
        if (!masterId.matches("[0-9a-zA-Z]{5}")) throw new IllegalArgumentException("""masterId Contains not Allowed Characters""")
        //if (!data.masterIdExists(masterId)) throw new IllegalArgumentException("""This masterId is not known""")
        if (!(pin >= 0 && pin < 64)) throw new IllegalArgumentException("""pin is not in range from 0-64""")
      case MHZ =>
        if (systemCode.length != 5) throw new IllegalArgumentException("""Length of systemCode is not 5""")
        if (!systemCode.matches("[01]{5}")) throw new IllegalArgumentException("""systemCode Contains not Allowed Characters""")
        if (unitCode.length != 5) throw new IllegalArgumentException("""Length of unitCode is not 5""")
        if (!unitCode.matches("[01]{5}")) throw new IllegalArgumentException("""unitCode Contains not Allowed Characters""")
    }
    true
  }

  protected def idTestIpc(id: String): Unit = {
    if (id.length != 5) throw new IllegalArgumentException("""Length of ID is not 5""")
    if (!id.matches("[-_a-zA-Z0-9]{5}")) throw new IllegalArgumentException("""ID Contains not Allowed Characters""")
  }

  type response <: IPCResponse
}

case class ipcAddControllerCommand(name: String) extends IPCCommand {
  override type response = ipcSuccessResponse}

case class ipcAddDeviceCommand(id: String, switchType: switchType, name: String, systemCode: String = "", unitCode: String = "", controlType: controlType, keepState: Boolean, pin: Int = 0, masterId: String = "") extends IPCCommand {
  idTestIpc(id)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)
  override type response = ipcSuccessResponse
}

case class ipcAddUserCommand(username: String, passHash: String) extends IPCCommand {override type response = ipcSuccessResponse}

case class ipcAddUserBase64Command(userName: String, passHashBase64: String) extends IPCCommand {
  def passHash: String = {
    new String(Base64.getDecoder.decode(passHashBase64))
  }
  override type response = ipcSuccessResponse
}

case class ipcGetControllerCommand() extends IPCCommand {
  type response = ipcGetControllerResponse
}

case class ipcGetControllerKeyCommand(id: String) extends IPCCommand {
  type response = ipcGetControllerKeyResponse
}

case class ipcGetDeviceCommand(id: String) extends IPCCommand {
  idTestIpc(id)
  type response = ipcGetDeviceResponse
}

case class ipcGetDevicesCommand() extends IPCCommand {
  type response = ipcGetDevicesResponse
}

case class ipcGetRandomId() extends IPCCommand {
  type response = ipcGetRandomIdResponse
}

case class ipcOffCommand(id: String) extends IPCCommand {
  idTestIpc(id)
  override type response = ipcSuccessResponse
}

case class ipcOnCommand(id: String, percent: Float) extends IPCCommand {
  if (percent < 0 || percent > 1) throw new IllegalArgumentException(s"percent must be between 0 and 1 was $percent")
  idTestIpc(id)

  override type response = ipcSuccessResponse
}

case class ipcProgramControllerCommand(path: String, masterId: String, ssid: String, pass: String) extends IPCCommand{override type response = ipcSuccessResponse}

case class ipcRecreateDatabase() extends IPCCommand{override type response = ipcSuccessResponse}

case class ipcSafeCreateDatabase() extends IPCCommand{override type response = ipcSuccessResponse}

case class ipcUpdateDeviceCommand(oldId: String, newId: String, keepState: Boolean, name: String, controlType: controlType, switchType: switchType, systemCode: String = "", unitCode: String = "", pin: Int = 0, masterId: String = "") extends IPCCommand {

  idTestIpc(newId)
  idTestIpc(oldId)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)

  override type response = ipcSuccessResponse
}

case class ipcCloseCommand() extends IPCCommand

case class ipcPingCommand() extends IPCCommand {
  type response = ipcPingResponse
}