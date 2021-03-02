package PrivateHome

import PrivateHome.UI.GUI.gui
import PrivateHome.UI.repl
import org.slf4j.LoggerFactory
import sun.misc.{Signal, SignalHandler}


object privatehome {
  val portable: Boolean = !getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath.startsWith("/usr/share/privatehome/")

  if (portable)
    System.setProperty("log.logpath","./logs/")
  else
    System.setProperty("log.logpath","/var/log/")
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    logger.info("Server gets started")

    gui
    data
    val repl = new repl()
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
    sys.exit(exitCode)
  }

}
