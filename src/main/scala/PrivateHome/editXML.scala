package PrivateHome

/**
 * This class provides the basic functionality to edit the XML files in this project.
 */

import scala.xml._

@deprecated("Will be removed soon", "0.3.1")
class editXML {

  /**
   * This method allows you to save the status of a switch with an id to the status.xml.
   * @param id is the id for the status (like the switch) you want to save
   * @param status is the status you want to save
   */
  def setStatus(id: String, status: Float): Unit = {
    val data: Elem = XML.load("src/main/scala/PrivateHome/status.xml")
    val prep: Elem = <device id={id}>{status}</device>
    val children: NodeSeq = data.child.foldLeft(NodeSeq.Empty)((acc, elem) => if ((elem \ "@id").text == id) acc else acc ++ elem)
    var result: Node = data.copy(child = children)
    result = addANode(result, prep)
    XML.save("src/main/scala/PrivateHome/status.xml", result)
  }

  /**
   * This method allows you to load data from the devices.xml file.
   *
   * @param id String to identify the node
   * @return The node from the XML-file
   */
  def loadObject(id: String): NodeSeq =  {
    val bigData = XML.load("src/main/scala/PrivateHome/devices.xml")
    val result = bigData.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (!((elem \ "@id").text == id)) acc else acc ++ elem)
    result
  }

  /** This method allows you to save changes to the devices.xml file.
   *
   * @param input The Elem you want to save
   * @param id    String to identify the node
   */
  def saveObject(input: Elem, id: String): Unit = {
    removeObject(id)
    addElem(input, id)
  }

  /** This method allows you to add a new device to the devices.xml and id.xml.
   *
   * @param input The Elem you want to add
   * @param id    String to identify the node
   */
  def addElem(input: Elem, id: String): Unit = {
    val device = XML.load("src/main/scala/PrivateHome/devices.xml")
    val idXml = XML.load("src/main/scala/PrivateHome/id.xml")

    val id_prep = <id>
      {id}
    </id>
    val id_result = addANode(idXml, id_prep)
    XML.save("src/main/scala/PrivateHome/id.xml", id_result)

    val result = addANode(device, input)
    XML.save("src/main/scala/PrivateHome/devices.xml", result)
  }

  /** This method is only for internal usage.
   *
   * @param to      The node to which the other node is getting added
   * @param newNode The new node, which is added to the main node.
   * @return The result of adding newNode at the end of to
   */
  //noinspection ScalaDeprecation
  private def addANode(to: Node, newNode: Node): Node = to match {
    case Elem(prefix, label, attributes, scope, child@_*) => Elem(prefix, label, attributes, scope,true, child ++ newNode: _*)
    case _ => println(" could not find node ")
      to
  }

  /** This method allows you to remove data from the devices.xml and the id.xml.
   *
   * @param id String to identify the node
   */
  def removeObject(id: String): Unit = {
    val devices = XML.load("src/main/scala/PrivateHome/devices.xml")
    val idXml = XML.load("src/main/scala/PrivateHome/id.xml")

    val children = devices.child.foldLeft(NodeSeq.Empty)((acc, elem) => if ((elem \ "@id").text == id) acc else acc ++ elem)
    val result = devices.copy(child = children)

    val id_prep = <id>
      {id}
    </id>
    val id_children = deleteNodes(idXml, elem => elem.text == id_prep.text)
    val id_result = idXml.copy(child = id_children)

    XML.save("src/main/scala/PrivateHome/id.xml", id_result)
    XML.save("src/main/scala/PrivateHome/devices.xml", result)
  }

  /** This method is only for internal usage.
   *
   * @param n The long list with all items
   * @param f The Elem which you wants to delete
   * @return The result of deleting Node f from Elem n
   */
  private def deleteNodes(n: Elem, f: Node => Boolean): NodeSeq = n.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (f(elem)) acc else acc ++ elem)
}
