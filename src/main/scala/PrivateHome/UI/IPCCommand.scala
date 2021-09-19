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
      case Mqtt =>
        if (masterId.length != 5) throw new IllegalArgumentException("""Length of masterId is not 5""")
        if (!masterId.matches("[0-9a-zA-Z]{5}")) throw new IllegalArgumentException("""masterId Contains not Allowed Characters""")
        //if (!data.masterIdExists(masterId)) throw new IllegalArgumentException("""This masterId is not known""")
        if (!(pin >= 0 && pin < 64)) throw new IllegalArgumentException("""pin is not in range from 0-64""")
      case Mhz =>
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
}

case class ipcAddControllerCommand(name: String) extends IPCCommand

case class ipcAddDeviceCommand(id: String, switchType: switchType, name: String, systemCode: String = "", unitCode: String = "", controlType: controlType, keepState: Boolean, pin: Int = 0, masterId: String = "") extends IPCCommand {
  idTestIpc(id)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)
}

case class ipcAddUserCommand(username: String, passHash: String) extends IPCCommand

case class ipcAddUserBase64Command(userName: String, passHashBase64: String) extends IPCCommand {
  def passHash: String = {
    new String(Base64.getDecoder.decode(passHashBase64))
  }
}

case class ipcGetControllerCommand() extends IPCCommand

case class ipcGetControllerKeyCommand(id: String) extends IPCCommand

case class ipcGetDeviceCommand(id: String) extends IPCCommand {
  idTestIpc(id)
}

case class ipcGetDevicesCommand() extends IPCCommand

case class ipcGetRandomId() extends IPCCommand

case class ipcOffCommand(id: String) extends IPCCommand

case class ipcOnCommand(id: String, percent: Float) extends IPCCommand

case class ipcProgramControllerCommand(path: String, masterId: String, ssid: String, pass: String) extends IPCCommand

case class ipcRecreateDatabase() extends IPCCommand

case class ipcSafeCreateDatabase() extends IPCCommand

case class ipcUpdateDeviceCommand(oldId: String, newId: String, keepState: Boolean, name: String, controlType: controlType, switchType: switchType, systemCode: String = "", unitCode: String = "", pin: Int = 0, masterId: String = "") extends IPCCommand {

  idTestIpc(newId)
  idTestIpc(oldId)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)

}

case class ipcCloseCommand() extends IPCCommand

case class ipcPingCommand() extends IPCCommand