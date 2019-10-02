package PrivateHome.Devices.MHz


object mhzCommand {
  def apply(systemCode: String, unitCode: String, command: Boolean): mhzCommand = new mhzCommand(systemCode, unitCode, command)
}

case class mhzCommand(systemCode: String, unitCode: String, command: Boolean)
