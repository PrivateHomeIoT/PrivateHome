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

import PrivateHome.Devices.switchType.switchType

trait IPCResponse extends Serializable {

}

case class ipcGetDevicesResponse(DeviceList: Map[String, ipcShortSwitchData]) extends IPCResponse

case class ipcGetDeviceResponse(device: ipcLongSwitchData) extends IPCResponse

case class ipcGetControllerResponse(controller: Map[String, String]) extends IPCResponse

case class ipcGetRandomIdResponse(id: String) extends IPCResponse

case class ipcGetControllerKeyResponse(key: Array[Byte]) extends IPCResponse

case class ipcSuccessResponse(command: IPCCommand, exception: Throwable = new Throwable, success: Boolean = true) extends IPCResponse

case class ipcPingResponse() extends IPCResponse

case class ipcShortSwitchData(id: String, dimmable: Boolean, name: String, status: Float)

case class ipcLongSwitchData(id: String, dimmable: Boolean, name: String, status: Float, switchType: switchType)