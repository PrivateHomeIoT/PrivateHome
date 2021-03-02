package PrivateHome

import PrivateHome.UI.GUI.gui
import PrivateHome.UI.repl

object privatehome {
  val portable: Boolean = !getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath.startsWith("/usr/share/privatehome/")

  def main(args: Array[String]): Unit = {

    gui
    data
    val repl = new repl()
  }

  def shutdown(exitCode: Int = 0): Unit = {
    repl.shutdown
    sys.exit(exitCode)
  }

}
