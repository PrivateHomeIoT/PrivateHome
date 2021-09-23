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

import java.io._



object repl {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val socketPath = "/tmp/privatehome2.sock"
  val serverSocket = new UnixDomainServerSocket(socketPath)

  class readThread extends Thread {
    var out: ObjectOutputStream = _
    var in: ObjectInputStream = _
    setName("replReadThread")

    override def run(): Unit = {
      while (true) {
        val clientSocket = serverSocket.accept()
        try {
          out = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream))
          out.flush()
          in = new ObjectInputStream(clientSocket.getInputStream)
          var line: IPCCommand = null
          do {
            line = in.readObject().asInstanceOf[IPCCommand]
            if (line != null) {
              var answer = stringCommandHandler.interpretMessage(line)
              if (answer == null) {
                answer = ipcSuccessResponse(line)
              }
              out.writeObject(answer)
              out.flush()
              logger.debug("Wrote Answer")
              //              answer match {
              //                case _: Exception => logger.warn("Will drop the exception")
              //                case list: List[(String,String)] => out.writeChars(list.map(tupel => s"${tupel._1}:${tupel._2}").mkString(","))
              //                case ans:IPCResponse => out.writeObject(ans)
              //              }
            }
          } while (line != null && !line.isInstanceOf[ipcCloseCommand])
          println("Exit")
        } catch {
          case e: IOException =>
          case e: ClassNotFoundException => in.readAllBytes()
            logger.warn("Class send by console not known.", e)
          case e: InvalidClassException => in.readAllBytes()
            logger.warn("Error while reading object from console.", e)
          case e: OptionalDataException => in.readAllBytes()
            logger.warn("Error while reading Objekt because it was no Object.", e)
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

