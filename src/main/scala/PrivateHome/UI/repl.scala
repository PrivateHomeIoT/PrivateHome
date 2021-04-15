package PrivateHome.UI


import org.scalasbt.ipcsocket.UnixDomainServerSocket
import org.slf4j.LoggerFactory

import java.io._



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
              out.println(stringCommandHandler.interpretMessage(line))
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

