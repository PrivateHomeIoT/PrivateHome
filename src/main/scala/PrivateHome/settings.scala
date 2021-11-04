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


import PrivateHome.UI.uiControl.formats
import PrivateHome.privatehome.portable
import org.json4s.JsonAST
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import java.io.{File, FileNotFoundException, PrintWriter}
import scala.io.Source

object settingsPath {
  var _path = "/etc/privatehome/settings.json"

  if (portable)
    _path = "settings.json"

  def path: String = _path
  def path_=(newPath:String): Unit = _path = newPath
}

case object settings {
  private val logger = LoggerFactory.getLogger(this.getClass)
  var websocket: httpSetting = httpSetting(port = 2888, "ws")
  var http: httpSetting = httpSetting(2000, "Website") //ToDo: change to 80 in produktion
  var database = new databaseSetting(userName = "user", pass = "pass", "/etc/privatehome/data/Devices")
  var mqtt: mqttBrokerSetting = mqttBrokerSetting("localhost", 1500)
  var keystore = new keystoreSetting("/Users/maximilian/Dokumente/GitHub/PrivateHome/src/main/resources/keystore.pkcs12","password")
  def Path:String = {settingsPath.path }


  load()

  def load(): Unit = {
    try {
      val fSource = Source.fromFile(Path)
    }
    catch {
      case e: FileNotFoundException => logger.warn("settings.json doesn't exist. Saving standard to create a new one.")
        save()
      case e: Throwable => throw e
    }

    val fSource = Source.fromFile(Path)
    var ftext: String = ""
    for (line <- fSource.getLines())
      ftext = ftext + line
    val jsonobj = parse(ftext)
    val web = jsonobj \ "websocket"
    websocket = web.extract[httpSetting]

    val httpJson = jsonobj \ "http"
    http = httpJson.extract[httpSetting]

    val databaseJson = jsonobj \ "database"
    database = databaseJson.extract[databaseSetting]

    val mqttJson = jsonobj \ "mqtt"
    mqtt = mqttJson.extract[mqttBrokerSetting]

    val keystoreJson = jsonobj \ "keystore"
    keystore = keystoreJson.extract[keystoreSetting]


  }

  def save(): Unit = {
    val writer = new PrintWriter(new File(Path))
    writer.write(pretty(render(("websocket" -> websocket.serialized) ~ ("http" -> http.serialized) ~ ("database" -> database.serialized) ~ ("mqtt" -> mqtt.serialized) ~ ("keystore" -> keystore.serialized))))
    writer.close()
  }

  trait settings

}

case class httpSetting(var port: Int, var path: String = "") extends setting {
  if (port < 0 || port > 0xffff) throw new IllegalArgumentException("Argument Port out of bound must be between 0x0 and 0xffff=65536")

  def serialized: JsonAST.JObject = {
    ("port" -> port) ~ ("path" -> path)
  }
}

class databaseSetting(var userName: String, pass: String, var path: String) extends setting {
  private var _passwordChar = pass.toCharArray
  def password_= (pass: String): Unit = {
    _passwordChar = pass.toCharArray
  }

  def password_= (pass: Array[Char]): Unit = _passwordChar = pass
  def password: Array[Char] = _passwordChar
  def passwordString: String = password.mkString("")
  override def serialized: JsonAST.JObject = ("userName" -> userName) ~ ("password" -> password.mkString("")) ~ ("path" -> path)

}

case class mqttBrokerSetting(var url: String, var port: Int) extends setting {
  if (port < 0 || port > 0xffff) throw new IllegalArgumentException("Argument Port aut of bound must be between 0x0 and 0xffff=65536")

  override def serialized: JsonAST.JObject = ("url" -> url) ~ ("port" -> port)
}

class keystoreSetting(var path: String, pass: String) extends setting {

  private var _passwordChar = pass.toCharArray
  def password_= (pass: String): Unit = {
    _passwordChar = pass.toCharArray
  }

  def password_= (pass: Array[Char]): Unit = _passwordChar = pass
  def password: Array[Char] = _passwordChar
  def passwordString: String = password.mkString("")


  override def serialized: JsonAST.JObject = ("path" -> path) ~ ("pass" -> passwordString)
}

abstract class setting {
  def serialized: JsonAST.JObject
}
