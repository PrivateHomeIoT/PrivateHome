package PrivateHome

import PrivateHome.UI.GUI.gui
import PrivateHome.UI.repl

object privatehome {
  def main(args: Array[String]): Unit = {

    gui
    data
    val repl = new repl()
  }
}
