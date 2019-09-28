package PrivateHome

/**
 * This class provides the basic functionality to edit the XML files in this project.
 */

import scala.xml._

class editXML {

  /** This method allows you to load data from the devices.xml file. All you need is a id as a String. This method returns a NodeSeq. */
  def loadObject(id: String) =  {
    val bigData = XML.load("src/main/scala/PrivateHome/devices.xml")
    val result = bigData.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (!((elem \ "@id").text == id)) acc else acc ++ elem)
    result
  }

  /** This method allows you to save changes to the devices.xml file. All you need is a Elem (like the output of loadObject) and the id of this Elem. */
  def saveObject(input: Elem, id: String): Unit = {
    removeObject(id)
    addElem(input, id)
  }

  /** This method allows you to add a new device to the devices.xml and id.xml. All you need is a Elem (like the output of loadObject) and a id of this Elem. */
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

  /** This method is only for internal usage. */
  private def addANode(to: Node, newNode: Node): Node = to match {
    case Elem(prefix, label, attributes, scope, child@_*) => Elem(prefix, label, attributes, scope, child ++ newNode: _*)
    case _ => println(" could not find node ")
      to
  }

  /** This method allows you to remove data from the devices.xml and the id.xml. All you need is the id of the data you want to remove finally. */
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

  /** This method is only for internal usage. */
  private def deleteNodes(n: Elem, f: Node => Boolean): NodeSeq = n.child.foldLeft(NodeSeq.Empty)((acc, elem) => if (f(elem)) acc else acc ++ elem)
}
