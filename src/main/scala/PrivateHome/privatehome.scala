package PrivateHome

import PrivateHome.UI.GUI.gui
import PrivateHome.UI.repl

object privatehome {
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
