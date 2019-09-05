package PrivateHome.Devices

import scala.xml._

abstract class Switch(private val setupID:String, setupKeepStatus:Boolean) {


  if (setupID.length != 32) throw new IllegalArgumentException("""Length of ID is not 32""")
  if (!setupID.matches("[-a-zA-Z0-9]{32}")) throw new IllegalArgumentException("""ID Contains not Allowed Characters""")
  //TODO: Control that the ID isn't used yet

  private var _status = false

  def on():Unit
  def off():Unit

  def Status(boolean: Boolean): Unit = {
    _status = boolean
    if (setupKeepStatus) {} //TODO: Trigger Status save
  }
  def Status_():Boolean = _status
  def id(): String = setupID

  def toXml():Node

}

object Switch {
  def apply(switchType:String,data:Node): Unit = {
    switchType match {
      case "433_MHz" =>
    }
  }
}