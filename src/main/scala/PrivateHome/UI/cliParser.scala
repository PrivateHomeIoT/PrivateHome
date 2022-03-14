/*
 * PrivateHome
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

package PrivateHome.UI

import PrivateHome.BuildInfo
import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

class cliParser(programName: String, arguments: Seq[String]) extends ScallopConf(arguments) {
  version(s"Version: ${BuildInfo.version}")
  banner(s"""Usage: $programName subcommand [options]""")

  val interactive: ScallopOption[Boolean] = toggle(default = Option(false))

  addSubcommand(new status(interactive))
  addSubcommand(new on(interactive))
  addSubcommand(new off(interactive))
  addSubcommand(new toggleSwitch(interactive))


  verify()


}

class status(interactive: ScallopOption[Boolean]) extends subcommandWithId(interactive, "status")

class toggleSwitch(interactive: ScallopOption[Boolean]) extends subcommandWithId(interactive, "toggle")

class on(interactive: ScallopOption[Boolean]) extends subcommandWithId(interactive, "on") {
  private val percentage: ScallopOption[Int] = opt[Int](name = "percentage", validate = per => {
    per > 0 && per <= 100
  })
  private val percentFloat: ScallopOption[Float] = opt[Float](name = "percent-float", short = 'f', validate = float => {
    float > 0 && float <= 1
  })

  def percent: Float = {
    percentFloat.getOrElse(percentage.getOrElse(100) / 100f)
  }

  mutuallyExclusive(percentage, percentFloat)
}

class off(interactive: ScallopOption[Boolean]) extends subcommandWithId(interactive, "off") {
}

abstract class subcommandWithId(interactive: ScallopOption[Boolean], subcommandName: String) extends Subcommand(subcommandName) {
  val id: ScallopOption[String] = opt[String]()

  validateOpt(interactive, id) { (i, id) => {
    if (!i.get && id.isEmpty) {
      Left("When the command is not run interactive you mus specify the id")
    } else {
      Right()
    }
  }
  }
}