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

package PrivateHome

import PrivateHome.Devices.MHz.sendMhz
import PrivateHome.Devices.MQTT.mqttClient
import PrivateHome.UI.GUI.gui
import PrivateHome.UI.repl
import org.slf4j.LoggerFactory
import sun.misc.{Signal, SignalHandler}


object privatehome {
  val portable: Boolean = !getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath.startsWith("/usr/share/privatehome/")

  val logCommands = System.getProperty("logCommands","notSpecified")

  if (logCommands.equals("ALLOW") || logCommands.equals("NATURAL") || logCommands.equals("DENY")) {

    System.setProperty("MARKER.COMMAND",logCommands)
  } else if (!logCommands.equals("notSpecified")) {
    System.setProperty("MARKER.COMMAND","ALLOW")
  }

  if (portable)
    System.setProperty("log.logpath","./logs/")
  else
    System.setProperty("log.logpath","/var/log/")
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    logger.info("Server is starting")

    gui
    data
    repl
    logger.debug("Registering Shutdown Handler")
    val intHandler = new SignalHandler {
      override def handle(signal: Signal): Unit = {
        shutdown()
      }
    }
    Signal.handle(new Signal("INT"),intHandler)
    logger.info("Server started")
  }

  def shutdown(exitCode: Int = 0): Unit = {
    logger.info("Shutting down Server")
    repl.shutdown
    gui.shutdown
    data.shutdown
    sendMhz.shutdown
    mqttClient.shutdown
    sys.exit(exitCode)
  }

}
