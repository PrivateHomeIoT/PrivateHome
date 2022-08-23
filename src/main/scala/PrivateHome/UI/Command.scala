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

import PrivateHome.Devices.{controlType, switchType}
import PrivateHome.Devices.switchType._
import PrivateHome.data
import PrivateHome.data.idTest

import java.util.Base64

class Command() extends Serializable {
  def testArgumentsSwitch(switchType: switchType, masterId: String, pin: Int, systemCode: String, unitCode: String): Boolean = {
    switchType match {
      case MQTT =>
        if (masterId.length != 5) throw new IllegalArgumentException("""Length of masterId is not 5""")
        if (!masterId.matches("[0-9a-zA-Z]{5}")) throw new IllegalArgumentException("""masterId Contains not Allowed Characters""")
        if (!data.masterIdExists(masterId)) throw new IllegalArgumentException("""This masterId is not known""")
        if (!(pin >= 0 && pin < 64)) throw new IllegalArgumentException("""pin is not in range from 0-64""")
      case MHZ =>
        if (systemCode.length != 5) throw new IllegalArgumentException("""Length of systemCode is not 5""")
        if (!systemCode.matches("[01]{5}")) throw new IllegalArgumentException("""systemCode Contains not Allowed Characters""")
        if (unitCode.length != 5) throw new IllegalArgumentException("""Length of unitCode is not 5""")
        if (!unitCode.matches("[01]{5}")) throw new IllegalArgumentException("""unitCode Contains not Allowed Characters""")
    }
    true
  }
}

case class commandOn(id: String, private var percent: String) extends Command {
  idTest(id)
  val percentFloat: Float = percent.toFloat
  if (0 > percentFloat || percentFloat > 1) throw new IllegalArgumentException("percent has to be between 0 and 1")
}

case class commandOff(id: String) extends Command {
  idTest(id)
}

case class commandProgramController(path: String, masterId: String, ssid: String, pass: String) extends Command {
  override def toString: String = {
    val msgSsid = if (ssid.isBlank) " "
    val msgPass = if (pass.isBlank) " "
    "commandProgramController(%s,%s,%s,%s)".format(path,masterId,msgSsid, msgPass)
  }
}

case class commandGetDevices() extends Command

case class commandSettingsMain(setting: String, value: AnyVal) extends Command

case class commandSettingsDevice(id: String, setting: String, value: AnyVal) extends Command {
  idTest(id)

}

case class commandAddDevice(id: String, switchType: switchType, name: String, systemCode: String = "", unitCode: String = "", controlType: controlType, keepState: Boolean, pin: Int = 0, masterId: String = "") extends Command {
  idTest(id, create = true)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)
}

case class commandAddController(name: String) extends Command

case class commandGetController() extends Command

case class commandUpdateDevice(oldId: String, newId: String, keepState: Boolean, name: String, controlType: controlType, switchType: switchType, systemCode: String = "", unitCode: String = "", pin: Int = 0, masterId: String = "") extends Command {

  idTest(newId, create = oldId != newId)
  idTest(oldId)
  testArgumentsSwitch(switchType, masterId, pin, systemCode, unitCode)

}

case class commandGetDevice(id: String) extends Command {
  idTest(id)
}

case class commandAddUserBase64(userName: String, passHashBase64: String) extends Command {
  def passHash: String = {
    new String(Base64.getDecoder.decode(passHashBase64))
  }
}

case class commandRecreateDatabase() extends Command

case class commandSafeCreateDatabase() extends Command

case class commandGetRandomId() extends Command

