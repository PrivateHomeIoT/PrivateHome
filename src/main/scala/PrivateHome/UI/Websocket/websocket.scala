package PrivateHome.UI.Websocket

import PrivateHome.UI._
import PrivateHome.data
import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import com.fasterxml.jackson.core.JsonParseException
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import org.json4s
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Calendar

object websocket {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private var ConnectionMap: Map[String, TextMessage => Unit] = Map()
  private var sessionMap: Map[String, session] = Map()

  /**
   * Main message and connection handler
   *
   * @param websocketId should be a unique ID for this websocket connection
   * @return A Flow for the akka system based on this logic
   */
  def listen(websocketId: String): Flow[Message, Message, NotUsed] = {
    var currentSession: session = session("", "", startInvalid = true)

    val inbound: Sink[Message, Any] = Sink.foreach(msg => {
      try {
        val msgText = msg.asTextMessage.getStrictText
        if (msgText.trim != "") { //In case some one sends an empty message only containing whitespaces because otherwise the execution fails without exception
          val json = parse(msgText)
          if (currentSession.resetValidUntil) {
            val commandType = (json \ "Command").extract[String]
            val args = json \ "Args"
            val answer = commandType match {
              case "on" => uiControl.receiveCommand(args.extract[commandOn])
              case "off" => uiControl.receiveCommand(args.extract[commandOff])
              case "getDevices" => uiControl.receiveCommand(args.extract[commandGetDevices])
              case "settingsMain" => uiControl.receiveCommand(args.extract[commandSettingsMain])
              case "settingsDevice" => uiControl.receiveCommand(args.extract[commandSettingsDevice])
              case "addDevice" => uiControl.receiveCommand(args.extract[commandAddDevice])
              case "getDevice" => uiControl.receiveCommand(args.extract[commandGetDevice])
              case e => sendMsg(websocketId, ("error" -> "Unknown Command") ~ ("command" -> e) ~ ("msg" -> msgText))
            }
            answer match {
              case jObject: JObject => sendMsg(websocketId, ("Command" -> commandType) ~ ("answer" -> jObject))
              case _ => sendMsg(websocketId, ("Command" -> commandType) ~ ("answer" -> "Success")) //This ensures that this flow is completed and the source is cleaned so that new Messages can be handled
            }


          } else {
            val authType = (json \ "auth").extract[String]
            authType match {
              case "ID" =>
                val sessionId = (json \ "sessionID").extract[String]
                currentSession = sessionMap.getOrElse(sessionId, currentSession) //replaces currentSession only wen a sessionObject exists
              case "pass" =>
                val username = (json \ "username").extract[String]
                val pass = (json \ "pass").extract[String].toCharArray
                val argon2 = Argon2Factory.create(Argon2Types.ARGON2id)
                val hash = data.getUserHash(username)
                if (argon2.verify(hash, pass)) {
                  val random = new SecureRandom()
                  val sessionID = new BigInteger(32 * 5, random).toString(32) //  This generates a random String with length 32
                  currentSession = session(sessionID, username)
                  sessionMap += sessionID -> currentSession
                }
                argon2.wipeArray(pass)

            }

            if (currentSession.valid) sendMsg(websocketId, ("auth" -> authType) ~ ("sessionID" -> currentSession.sessionID) ~ ("authenticated" -> true))
            else sendMsg(websocketId, ("auth" -> authType) ~ ("authenticated" -> false))

          }

        }


      }
      catch {
        case exception: JsonParseException => println(exception); sendMsg(websocketId, ("error" -> "JsonParseException") ~ ("exception" -> exception.toString))
        case exception: Throwable => println(exception)
          exception.printStackTrace()
          sendMsg(websocketId, ("error" -> exception.getCause.toString) ~ ("exception" -> exception.toString))
      }
    })

    val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

    Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {
      //outboundMat.offer is an access to the outbound Source of the Flow (returning to the Client)
      ConnectionMap += websocketId -> outboundMat.offer
      NotUsed
    })
  }

  /**
   * Send a message to a specific websocket client
   *
   * @param id  the ID of the websocket connection the one passed to listen
   * @param msg the message that should be send to the Client as a JSON object
   */
  def sendMsg(id: String, msg: json4s.JObject): Unit = {
    ConnectionMap(id).apply(TextMessage.Strict(compact(render(msg))))
  }

  /**
   * Sends a message to all connections
   *
   * @param msg The message that should be send to all connections as a JSON object
   */
  def broadcastMsg(msg: json4s.JObject) {
    for (connection <- ConnectionMap.values) connection(TextMessage.Strict(compact(render(msg))))
  }

  /**
   * Sends a message to all connections
   *
   * @param text The message that should be send to all connections as a string
   */
  def broadcastMsg(text: String): Unit = {
    for (connection <- ConnectionMap.values) connection(TextMessage.Strict(text))
  }


  /**
   * A case class that stores all information about an session and handels the session timeout
   *
   * @param sessionID the sessionID to use
   * @param username  the username used for authentication
   */
  private case class session(sessionID: String, username: String, private val startInvalid: Boolean = false) {
    private var validUntil: Calendar = nowPlus15
    if (startInvalid) validUntil = Calendar.getInstance()

    def _validUntil: Calendar = validUntil

    /**
     * Checks if this SessionID is valid and resets the time if no longer valid removes it self from memory
     *
     * @return True wen the SessionId is still valid
     */
    def resetValidUntil: Boolean = {
      if (valid) {
        validUntil = nowPlus15
        true
      }
      else {
        sessionMap -= sessionID
        false
      }

    }

    /**
     * Calculates the time wen this session should be invalidated
     *
     * @return the Calendar instance representing the first moment this should no longer be valid
     */
    def nowPlus15: Calendar = {
      val now = Calendar.getInstance()
      now.add(Calendar.MINUTE, 15)
      now
    }

    /**
     * Checks if this SessionID is valid
     *
     * @return If the sessionID is still valid
     */
    def valid: Boolean = validUntil.after(Calendar.getInstance())
  }

}