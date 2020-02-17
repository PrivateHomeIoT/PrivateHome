package PrivateHome

import PrivateHome.Devices.MHz.mhzSwitch
import org.scalatest.{Outcome, fixture}
import org.scalatest.fixture.FunSuite
import scalikejdbc._

class dataSQLTest extends fixture.FunSuite  {

  type FixtureParam = AutoSession

  override def withFixture(test: OneArgTest): Outcome = {
    Class.forName("org.h2.Driver")
    ConnectionPool.singleton("jdbc:h2:./daten/hello", "user", "pass")
    val session: AutoSession.type = AutoSession
    test(session)
  }

  test("getDevices") { psession =>
    implicit val session: AutoSession = psession
    dataSQL.create()
    dataSQL.addDevice(mhzSwitch("tesef", _keepStatus = true, "10000", "10101"))
    println(dataSQL.getDevices)
  }

}
