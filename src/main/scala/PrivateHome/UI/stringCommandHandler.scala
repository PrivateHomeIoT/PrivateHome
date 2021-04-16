package PrivateHome.UI

import PrivateHome.data
import org.slf4j.LoggerFactory

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
          var id: String = null
          val random = new SecureRandom()
          var run = true
          while (run) {

            id = new BigInteger(5 * 5, random).toString(32) //  This generates a random String with length 5
            try {
              data.idTest(id, create = true)
              logger.debug("Recomended id: \"{}\"",id)
              run = false
            }
            catch {
              case _: IllegalArgumentException =>
            }
          }
          id
        case "commandOn" => commandOn(args(0), args(1))
        case "commandOff" => commandOff(args(0))
        case "commandAddDevice" => commandAddDevice(args(0), args(1), args(2), args(3), args(4), args(5), args(6).toBoolean)

        case _ => logger.warn("Unknown Command")
          new Command
      }
      uiCommand match {
        case c:Command => uiControl.receiveCommand(c)
        case s:String => s
      }

    } catch {
      case e: Throwable => logger.error("Unknown Error while interpreting Console command",e)
        e + e.getStackTrace.mkString("\n")
    }
  }
}
