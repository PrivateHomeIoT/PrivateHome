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


import org.scalasbt.ipcsocket.UnixDomainServerSocket
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, File, IOException, InputStreamReader, PrintWriter}
import java.math.BigInteger
import java.nio.file.{Path, Paths}
import java.security.SecureRandom
import scala.concurrent.Future


object repl {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val socketPath = "/tmp/privatehome2.sock"
  val serverSocket = new UnixDomainServerSocket(socketPath)

  class readThread extends Thread {
    setName("replReadThread")
    override def run(): Unit = {
      while (true) {
        val clientSocket = serverSocket.accept()
        try {
          val out = new PrintWriter(clientSocket.getOutputStream, true)
          val in =
            new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
          var line: String = null
          do {
            line = in.readLine()
            if (line != null) {
              val answer = stringCommandHandler.interpretMessage(line)
              answer match {
                case _: Exception => logger.warn("Will drop the exception")
                case list: List[(String,String)] => out.println(list.map(tupel => s"${tupel._1}:${tupel._2}").mkString(","))
                case ans => out.println(ans)
              }
            }
          } while (line != null && !line.trim().equals("bye"))
        } catch {
          case _: IOException =>
        }
      }

    }
  }

  val replReadThread = new readThread

  replReadThread.start()
  logger.info("Started repl handler thread")

  def shutdown(): Unit ={
    logger.info("Shutting down repl")
    val socketfile = new File(socketPath)
    serverSocket.close()
    socketfile.delete()
    logger.info("REPL shut down")
  }
}

