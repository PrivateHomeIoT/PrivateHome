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

import PrivateHome.data
import PrivateHome.data.chars
import org.slf4j.LoggerFactory

import java.lang.Integer.parseInt
import java.math.BigInteger
import java.security.SecureRandom

object stringCommandHandler {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def interpretMessage(msg: String): Any = {
    try {
      val command: Array[String] = msg.stripSuffix(")").split('(')
      val args = if ((command.length == 2) && (command(1) != null)) command(1).split(',') else null
      logger.debug("command = {}", command(0))

      val uiCommand = command(0) match {
        case "commandAddUserBase64" =>
          commandAddUserBase64(args(0), args(1))
        case "commandRecreateDatabase" => commandRecreateDatabase()
        case "commandSafeCreateDatabase" => commandSafeCreateDatabase()
        case "getRandomId" =>
          logger.debug("Running getRandomId")
          var id: String = ""
          val random = new SecureRandom()
          var run = true
          while (run) {
            id = ""
            new BigInteger(5 * 6, random).toString(2).grouped(6).foreach(t => id += chars(parseInt(t,2))) //  This generates a random String with length 5 and the chars -_a-Z0-9
            try {
              data.idTest(id, create = true)
              logger.debug("Recomended id: \"{}\"", id)
              run = false
            }
            catch {
              case t: IllegalArgumentException => logger.debug("Error while generating randomID",t)
              case t: Exception => logger.warn("Error while generating randomID",t)
            }
          }
          id
        case "commandOn" => commandOn(args(0), args(1))
        case "commandOff" => commandOff(args(0))
        case "commandAddDevice" => commandAddDevice(args(0), args(1), args(2), args(3), args(4), args(5), args(6).toBoolean, args(7).toInt, args(8))
        case "commandAddController" => commandAddController(args(0))
        case "commandGetController" => commandGetController()
        case "getControllerKey" => data.getControllerMasterId(args(0)).keyArray.map(_ & 0xFF).mkString(",")  //"%02x" format _
        case "commandProgramController" =>
          commandProgramController(args(0), args(1), args(2), args(3))

        case _ => logger.warn("Unknown Command")
          new Command
      }
      uiCommand match {
        case c: Command => uiControl.receiveCommand(c)
        case s: String => s
        case _ => ""
      }

    } catch {
      case e: Throwable => logger.error("Unknown Error while interpreting Console command", e)
        e + e.getStackTrace.mkString("\n")
    }
  }
}
