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

package PrivateHome.UI

import org.rogach.scallop.exceptions.ValidationFailure
import org.rogach.scallop.throwError
import org.scalatest.funsuite.AnyFunSuite

class cliParserTest extends AnyFunSuite {
  throwError.value = false

  test("test Interactive default") {
    val conf = new cliParser("", List())
    assertResult(false)(conf.interactive())
  }

  test("Test Interactive") {
    val conf = new cliParser("", List("-i"))
    assert(conf.interactive())
  }

}

class onTest extends subcommandWithIdTest[on]("on") {
  throwError.value = true

  test("test percent float (short)") {
    val conf = new cliParser("", List("on", "--id", "aaaaa", "-f", "0.12"))
    val on = conf.subcommand.get match {
      case s: on => s
      case _ => cancel("This should not be reached")
    }

    assertResult(0.12f)(on.percent)
  }

  test("Test percent float (long)") {
    val conf = new cliParser("", List("on", "--id", "aaaaa", "--percent-float", "0.22"))
    val on = conf.subcommand.get match {
      case s: on => s
      case _ => cancel("This should not be reached")
    }

    assertResult(0.22f)(on.percent)
  }

  test("Test percentage (short)") {
    val conf = new cliParser("", List("on", "--id", "aaaaa", "-p", "22"))
    val on = conf.subcommand.get match {
      case s: on => s
      case _ => cancel("This should not be reached")
    }

    assertResult(0.22f)(on.percent)
  }

  test("Test percentage (long)") {
    val conf = new cliParser("", List("on", "--id", "aaaaa", "--percentage", "32"))
    val on = conf.subcommand.get match {
      case s: on => s
      case _ => cancel("This should not be reached")
    }

    assertResult(0.32f)(on.percent)
  }
}

class statusTest extends subcommandWithIdTest[status]("status")

class offTest extends subcommandWithIdTest[off]("off")

class toggleTest extends subcommandWithIdTest[toggleSwitch]("toggle")

class subcommandWithIdTest[toTest](command: String) extends AnyFunSuite {
  throwError.value = true

  test(s"Test subcommand create $command") {
    val conf = new cliParser("", List("-i", command))
    conf.subcommand.get match {
      case _: toTest => succeed
      case sup => fail(s"${sup.getClass} is not $command")
    }
  }

  test("Test need ID") {
    assertThrows[ValidationFailure](new cliParser("", List(command)))
  }


  test("Test id verify") {
    val conf = new cliParser("", List(command, "--id", "aaaaa"))
    val tog: toTest = conf.subcommand.get.asInstanceOf[toTest]

    //    this test will only work with classes inheriting from subcommandWithId
    tog match {
      case s: subcommandWithId =>
        assert(s.id.isSupplied)
        assertResult("aaaaa")(s.id())
      case _ => fail("The supplied command does not support id")
    }
  }

}