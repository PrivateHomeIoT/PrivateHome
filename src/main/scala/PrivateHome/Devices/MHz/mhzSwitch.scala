package PrivateHome.Devices.MHz

import PrivateHome.Devices.Switch

case class mhzSwitch(setupID: String, _KeepStatus: Boolean, private var _systemCode: String, private var _unitCode: String) extends Switch(setupID, _KeepStatus) {
  if (_systemCode.length != 5) throw new IllegalArgumentException(s"System code ${_systemCode} should be 5 Char long")
  if (_unitCode.length != 5) throw new IllegalArgumentException(s"Unit Code ${_unitCode} should be 5 Char long")

  if (!_systemCode.matches("[01]{5}")) throw new IllegalArgumentException(s"System code ${_systemCode} should only contain 0/1")
  if (!_unitCode.matches("[01]{5}")) throw new IllegalArgumentException(s"Unit code ${_unitCode} should only contain 0/1")

  def systemCode: String = _systemCode

  def systemCode_(newSystemCode: String): Unit = {
    if (_systemCode.length != 5) throw new IllegalArgumentException(s"System code ${_systemCode} should be 5 Char long")
    if (!_systemCode.matches("[01]{5}")) throw new IllegalArgumentException(s"System code ${_systemCode} should only contain 0/1")
    _systemCode = newSystemCode
  }

  def unitCode: String = _unitCode

  def unitCode_(newUnitCode: String): Unit = {
    if (_unitCode.length != 5) throw new IllegalArgumentException(s"Unit Code ${_unitCode} should be 5 Char long")
    if (!_unitCode.matches("[01]{5}")) throw new IllegalArgumentException(s"Unit code ${_unitCode} should only contain 0/1")
    _unitCode = newUnitCode
  }

  override def on(): Unit = queue.queue.enqueue(mhzCommand(_systemCode, _unitCode, command = true))

  override def off(): Unit = queue.queue.enqueue(mhzCommand(_systemCode, _unitCode, command = false))

}
