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

import PrivateHome.Devices.controlType._
import PrivateHome.Devices.switchType._
import org.scalasbt.ipcsocket.UnixDomainServerSocket
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.Waiters.Waiter
import org.scalatest.concurrent.Waiters
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, InvalidClassException, ObjectInputStream, ObjectOutputStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class consoleTest extends AnyFunSuite with BeforeAndAfter {
  private val consoleStdIn = console.StdInJava
  var serverSocket: UnixDomainServerSocket = _

  protected abstract class ipcThread(w: Waiters.Waiter, debugging: Boolean, t: Any*) extends Thread {
    protected var out: ObjectOutputStream = _
    protected var in: ObjectInputStream = _

    override def run(): Unit = {
      try {
        if (debugging)
          println("Test Thread Waits for connection")
        val clientSocket = serverSocket.accept()

        out = new ObjectOutputStream(clientSocket.getOutputStream)
        in = new ObjectInputStream(clientSocket.getInputStream)
        if (debugging)
          println("Test Thread Connected")
        testCode(t)
      } finally w.dismiss()
    }

    protected def read: IPCCommand = in.readObject().asInstanceOf[IPCCommand]

    protected def testCode(args: Any*): Unit
  }

  before({
    console.socketPath = "testing.sock"
    val socketFile = new File(console.socketPath)
    socketFile.delete()
    serverSocket = new UnixDomainServerSocket(console.socketPath)
  })

  after({
    val socketFile = new File(console.socketPath)
    socketFile.delete()
  })

  test("Test: send without connect") {
    assertThrows[IllegalStateException](console.send(new ipcPingCommand))
  }

  test("Test: Connect") {
    val w = new Waiter
    class readThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {}
    }

    serverSocket.setSoTimeout(200)


    val testThread = new readThread

    testThread.start()
    console.connect()

    w.await(Timeout(20 millis))

  }

  test("Test: send with disconnect") {
    val outputStream = new ByteArrayOutputStream()

    val w = new Waiter
    class connectThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {}
    }

    class sendThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        in.readObject().asInstanceOf[ipcPingCommand]
        out.writeObject(ipcPingResponse())
        val request = in.readObject().asInstanceOf[ipcPingCommand]
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new connectThread
    val testThread2 = new sendThread

    testThread.start()
    console.connect()
    console.socket.close()
    testThread2.start()

    Console.withOut(outputStream) {
      assertResult(ipcPingResponse())(console.send(new ipcPingCommand))
      assertResult("Server did not response with expected class")(intercept[InvalidClassException](console.send(ipcPingCommand())).getMessage)
    }

    assertResult("Connection is closed.\nReconnecting\n")(outputStream.toString())
    w.await(Timeout(20 millis))
  }

  test("Test: Send with fail") {
    val w = new Waiter
    val outputStream = new ByteArrayOutputStream()

    class sendTest extends ipcThread(w, false) {
      override protected def testCode(args: Any*): Unit = {
        val request = in.readObject().asInstanceOf[ipcPingCommand]
        val throwable = new IllegalArgumentException
        throwable.setStackTrace(Array(new StackTraceElement("TestClass", "FailTest", "consoleTest.scala", 1)))
        out.writeObject(ipcSuccessResponse(request, throwable, success = false))
      }
    }

    val testThread = new sendTest
    testThread.start()

    console.connect()
    Console.withErr(outputStream) {
      assertThrows[RuntimeException](console.send(ipcPingCommand()))
    }
    println(outputStream.toString())
    assertResult("command: ipcPingCommand() failed with exception:\njava.lang.IllegalArgumentException\n\tat TestClass.FailTest(consoleTest.scala:1)\n")(outputStream.toString())

    w.await()
  }

  test("Test: getControllerID") {

    val printStream = new ByteArrayOutputStream()
    val readStream = new ByteArrayInputStream("ccccc\n2\n1\n".getBytes)
    val errStream = new ByteArrayOutputStream()
    val w = new Waiter

    class ControllerAnswerThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val request = read
        w {
          assertResult(
            new ipcGetControllerCommand)(request)
        }
        out.writeObject(ipcGetControllerResponse(Map("aaaaa" -> "Test 1", "bbbbb" -> "Test 2")))
      }
    }

    val testThread = new ControllerAnswerThread
    testThread.start()

    console.connect()

    Console.withOut(printStream) {
      Console.withIn(readStream) {
        Console.withErr(errStream) {
          console.getControllerId
        }
      }
    }

    assertResult("Available Controller:\n0 aaaaa Test 1 \n1 bbbbb Test 2 \n\nChose Controller by number or ID \n> Chose Controller by number or ID \n> Chose Controller by number or ID \n> ")(printStream.toString)

    w.await()
  }

  test("Test: printControllerKey") {

    val printStream = new ByteArrayOutputStream()
    val readStream = new ByteArrayInputStream("0".getBytes)
    val errStream = new ByteArrayOutputStream()
    val w = new Waiter

    class ControllerAnswerThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        var request: Any = in.readObject().asInstanceOf[ipcGetControllerCommand]
        w {
          assertResult(
            new ipcGetControllerCommand)(request)
        }
        out.writeObject(ipcGetControllerResponse(Map("aaaaa" -> "Test 1", "bbbbb" -> "Test 2")))
        request = in.readObject().asInstanceOf[ipcGetControllerKeyCommand]
        w {
          assertResult(ipcGetControllerKeyCommand("aaaaa"))(request)
        }
        out.writeObject(ipcGetControllerKeyResponse(Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)))
      }
    }

    val testThread = new ControllerAnswerThread
    testThread.start()
    console.connect()

    Console.withOut(printStream) {
      Console.withIn(readStream) {
        Console.withErr(errStream) {
          console.printControllerKey()
        }
      }
    }

    assertResult("Available Controller:\n0 aaaaa Test 1 \n1 bbbbb Test 2 \n\nChose Controller by number or ID \n> 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16\n")(printStream.toString)

    w.await()
  }

  test("Test: safeCreate") {
    val w = new Waiter
    class createTest extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val request = in.readObject().asInstanceOf[ipcSafeCreateDatabase]
        w {
          assertResult(new ipcSafeCreateDatabase)(request)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new createTest
    testThread.start()

    console.connect()
    console.safeCreate()
    w.await()
  }

  test("Test: recreate") {
    val w = new Waiter
    class createTest extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val request = in.readObject().asInstanceOf[ipcRecreateDatabase]
        w {
          assertResult(new ipcRecreateDatabase)(request)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new createTest
    testThread.start()

    console.connect()
    console.recreate()
    w.await()
  }

  test("Test: add User") {
    val outputStream = new ByteArrayOutputStream()
    val inputStream = new ByteArrayInputStream("scalatest\nTestPassword\n".getBytes())
    val w = new Waiter

    class userTest extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val request = in.readObject().asInstanceOf[ipcAddUserCommand]
        w {
          assert(request.isInstanceOf[ipcAddUserCommand])
          assertResult("scalatest")(request.username)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new userTest
    testThread.start()
    console.StdInJava = null

    console.connect()


    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        console.addUser()
      }
    }


    assertResult("Please provide Username and Password (does not get echoed)\nUsername> Because System.console does not exist the password will get echoed.\nPassword> ")(outputStream.toString)

    w.await()
  }

  test("Test: readPassword normally") {
    console.StdInJava = consoleStdIn
    assume(console.StdInJava != null)
    val prompt1 = "Password> "
    val password1 = "ThisIsThePassword"
    val outputStream = new ByteArrayOutputStream()
    val inputStream = new ByteArrayInputStream(s"$password1\n".getBytes())
    var answer: Future[Array[Char]] = null
    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        answer = Future {
          console.readPassword(prompt1)
        }
      }
    }
    Thread.sleep(100)
    if (answer.isCompleted) {
      fail(s"This command should have failed but got ${answer.value.get.get.mkString}")
    }
    succeed
  }

  test("Test: readPassword with unavailable stdin") {
    console.StdInJava = null

    val prompt1 = "Password> "
    val prompt2 = "Key>"
    val password1 = "ThisIsThePassword"
    val password2 = "ThisIsASecretKey"
    val outputStream = new ByteArrayOutputStream()
    val inputStream = new ByteArrayInputStream(s"$password1\n$password2\n".getBytes())

    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        assertResult(password1.toCharArray)(console.readPassword(prompt1))
        assertResult(password2.toCharArray)(console.readPassword(prompt2))
      }
    }
    assertResult(s"Because System.console does not exist the password will get echoed.\n${prompt1}Because System.console does not exist the password will get echoed.\n$prompt2")(outputStream.toString())
  }

  test("Test: addController") {
    val w = new Waiter

    val controllerName1 = "TestController1"
    val controllerName2 = "AnotherController"

    val inputStream = new ByteArrayInputStream(s"$controllerName1\n$controllerName2\n".getBytes())
    val outputStream = new ByteArrayOutputStream()

    class controllerThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        var request = in.readObject().asInstanceOf[ipcAddControllerCommand]
        println("Received first")
        w {
          assertResult(controllerName1)(request.name)
        }
        out.writeObject(ipcSuccessResponse(request))
        request = in.readObject().asInstanceOf[ipcAddControllerCommand]
        println("Received second")
        w {
          assertResult(controllerName2)(request.name)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new controllerThread
    testThread.start()

    console.connect()
    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        console.addController()
        console.addController()
      }
    }

    assertResult("Name> Name> ")(outputStream.toString())
    w.await()
  }

  test("Test: getDeviceID non-interactive") {
    val w = new Waiter

    val device = List(ipcShortSwitchData("12345", dimmable = true, "Test Device1", 0.1f), ipcShortSwitchData("ab345", dimmable = false, "Test Device 2", 1)).map(s => s.id -> s).toMap

    class deviceThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val answer = read
        w {
          assert(answer.isInstanceOf[ipcGetDevicesCommand])
        }
        out.writeObject(ipcGetDevicesResponse(device))
        out.writeObject(ipcGetDevicesResponse(device))
      }
    }

    val testThread = new deviceThread
    testThread.start()

    console.interactive = false
    console.connect()
    val idWanted = device.keys.head
    assertResult(idWanted)(console.getDeviceID(idWanted))
    assertThrows[IllegalArgumentException](console.getDeviceID())

    w.await()
  }

  test("Test: getDeviceID interactive") {
    val w = new Waiter

    val outputStream = new ByteArrayOutputStream()
    val inputStream = new ByteArrayInputStream(s"2\nab345".getBytes())

    val device = List(ipcShortSwitchData("12345", dimmable = true, "Test Device1", 0.1f), ipcShortSwitchData("ab345", dimmable = false, "Test Device 2", 1)).map(s => s.id -> s).toMap

    class deviceThread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        val answer = in.readObject()
        w {
          assert(answer.isInstanceOf[ipcGetDevicesCommand])
        }
        out.writeObject(ipcGetDevicesResponse(device))
        out.writeObject(ipcGetDevicesResponse(device))
      }
    }

    val testThread = new deviceThread
    testThread.start()
    console.connect()
    console.interactive = true

    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        assertResult("ab345")(console.getDeviceID())
      }
    }

    w.await()

  }

  test("Test: programController") {
    val w = new Waiter

    val array = (10 to 45).mkString("")
    val outputStream = new ByteArrayOutputStream()
    val inputStream = new ByteArrayInputStream(s"0\n${array.substring(0, 33)}\n\n${array.substring(0, 65)}\n\n/\n\n".getBytes())

    class thread extends ipcThread(w, false) {
      protected override def testCode(args: Any*): Unit = {
        var request: IPCCommand = read
        w {
          assertResult(
            new ipcGetControllerCommand)(request)
        }
        out.writeObject(ipcGetControllerResponse(Map("aaaaa" -> "Test 1", "bbbbb" -> "Test 2")))
        request = read
        w {
          assertResult(ipcProgramControllerCommand("/dev/ttyUSB0", "aaaaa", "", ""))(request)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new thread
    testThread.start()

    console.connect()
    Console.withIn(inputStream) {
      Console.withOut(outputStream) {
        console.programController()
      }
    }

    w.await()
  }

  test("Test: status") {
    val w = new Waiter

    val devices = List(ipcShortSwitchData("12345", dimmable = true, "Test Device1", 0.1f), ipcShortSwitchData("ab345", dimmable = false, "Test Device 2", 1)).map(s => s.id -> s).toMap
    val outputStream = new ByteArrayOutputStream()

    class thread extends ipcThread(w, false) {
      override protected def testCode(args: Any*): Unit = {
        var answer = in.readObject()
        w {
          assert(answer.isInstanceOf[ipcGetDevicesCommand])
        }
        out.writeObject(ipcGetDevicesResponse(devices))
        answer = in.readObject()
        w {
          assertResult(ipcGetDeviceCommand("12345"))(answer)
        }
        out.writeObject(ipcGetDeviceResponse(ipcLongSwitchData("12345", dimmable = true, "Test Device1", 0.1f, MQTT)))
      }
    }

    val testThread = new thread
    testThread.start()

    console.connect()

    Console.withOut(outputStream) {
      console.status("12345")
    }

    assertResult("10.0\n")(outputStream.toString())
    w.await()
  }

  test("Test: addSwitch") {
    val w = new Waiter

    val inputStream = new ByteArrayInputStream(("1111\n" + // Test id Fail
      "\n" + // Test default
      "Test Create 1\n" + //Name
      "y\n" + //Test both keep states
      "0\n" + //Select controller
      "a\n-1\n64\n16\n" + //Test pin for format exception, negative and to large values
      "22222\n" + //Test correct id
      "Test Create 2\n" + //Name
      "n\n" + //Will let both checks for keepState fail
      "InvalidSwitchType\n" + //To check how a not registered String will get handled
      "433Mhz\n" + //should work since everything get to lower case
      "11111\n00000\n" + //since the default value already triggers the while loop we can use valid ones
      "").getBytes())
    val outputStream = new ByteArrayOutputStream()
    val expectedOutput: String = "id[11111]> " + //id fail
      "id[11111]> " + //id default
      "name> " +
      "Keep State (y/n)> " + // Test both
      "Available Controller:\n0 maste Test 1 \n1 bbbbb Test 2 \n\nChose Controller by number or ID \n> " + //getControllerId
      "Pin number 0-63> Pin number 0-63> Pin number 0-63> Pin number 0-63> " + // 4 times for the for checks
      "id[22222]> " + // correct id
      "name> " +
      "Keep State (y/n)> " + //n
      "Control Type (mqtt/433mhz)> " + //InvalidSwitchType
      "Control Type (mqtt/433mhz)> " + //433Mhz
      "systemCode (00000 - 11111)> " + //systemCode 11111
      "unitCode (00000 - 11111)> " + //unitCode 00000
      ""
    class thread extends ipcThread(w, debugging = false) {
      override protected def testCode(args: Any*): Unit = {

        // Test MQTT
        var request = read
        w {
          assertResult(ipcGetRandomId())(request)
        }
        out.writeObject(ipcGetRandomIdResponse("11111"))
        request = read
        w {
          assertResult(
            new ipcGetControllerCommand)(request)
        }
        out.writeObject(ipcGetControllerResponse(Map("maste" -> "Test 1", "bbbbb" -> "Test 2")))
        request = read
        w {
          assertResult(ipcAddDeviceCommand("11111", MQTT, "Test Create 1", "null", "null", SLIDER, keepState = true, 16, "maste"))(request)
        }
        out.writeObject(ipcSuccessResponse(request))

        //Test MHZ
        request = read
        w {
          assertResult(ipcGetRandomId())(request)
        }
        out.writeObject(ipcGetRandomIdResponse("22222"))
        request = read
        w {
          assertResult(ipcAddDeviceCommand("22222", MHZ, "Test Create 2", "11111", "00000", BUTTON, keepState = false, -1, null))(request)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new thread
    testThread.start()

    console.connect()

    Console.withOut(outputStream) {
      Console.withIn(inputStream) {
        console.addSwitch(true)
        console.addSwitch(false)
      }
    }


    assertResult(expectedOutput)(outputStream.toString())
    w.await()
  }

  test("Test: dim") {
    val w = new Waiter

    val device = List(ipcShortSwitchData("11111", dimmable = true, "Test Device1", 0.1f), ipcShortSwitchData("aaaaa", dimmable = false, "Test Device 2", 1)).map(s => s.id -> s).toMap
    val inputStream = new ByteArrayInputStream("a\n0.1\n0".getBytes())

    class Thread extends ipcThread(w, false) {
      override def testCode(args: Any*): Unit = {
        var request = read
        w {
          assert(request.isInstanceOf[ipcGetDevicesCommand])
        }
        out.writeObject(ipcGetDevicesResponse(device))
        request = read
        w {
          assertResult(ipcOnCommand("11111",0.1f))(request)
        }
        out.writeObject(ipcSuccessResponse(request))

        request = read
        w {
          assert(request.isInstanceOf[ipcGetDevicesCommand])
        }
        out.writeObject(ipcGetDevicesResponse(device))
        request = read
        w {
          assertResult(ipcOffCommand("aaaaa"))(request)
        }
        out.writeObject(ipcSuccessResponse(request))
      }
    }

    val testThread = new Thread
    testThread.start()

    console.connect()

    Console.withIn(inputStream) {
      console.dim("11111")
      console.dim("aaaaa")
    }

    w.await()
  }
}


