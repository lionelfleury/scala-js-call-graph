package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{ClassNode, MethodNode, Node}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._
import scala.collection._

object Visualization extends JSApp {
  val classes = mutable.Set[ClassNode]()
  val methods = mutable.Set[MethodNode]()

  val fileInput = input(`type` := "file").render
  val box = input(`type` := "text", placeholder := "Type here to search !").render
  val exported = input(`type` := "checkbox", checked).render
  val searchField = div(box, " Only exported:", exported).render
  val output = span.render

  box.onkeyup = searchList _
  exported.onclick = searchList _

  def main(): Unit = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.appendChild(fileInput)
    fileInput.onchange = readFile(target) _
  }

  def readFile(target: Div)(evt: sdom.Event) = {
    evt.stopPropagation()
    target.innerHTML = ""
    target.appendChild(div(searchField, output).render)
    val reader = new FileReader()
    reader.readAsText(fileInput.files(0))
    reader.onload = (e: sdom.UIEvent) => {
      val text = reader.result.asInstanceOf[String]
      val nodes = upickle.read[Seq[Node]](text)
      for (node <- nodes) node match {
        case node: MethodNode => methods += node
        case node: ClassNode => classes += node
      }
      sdom.console.log("Classes:" + classes.size)
      sdom.console.log("Methods:" + methods.size)

      searchList(e)
      D3Graph.renderGraph(classes.filter(_.isExported).toSeq, methods.toSeq)
    }
  }

  def renderList = ul(
    for {
      node <- classes.toSeq
      if (if (exported.checked) node.isExported else true) &&
        node.displayName.toLowerCase.contains(box.value.toLowerCase)
    } yield li(node.displayName, onclick := view _)
  ).render

  def view(evt: sdom.MouseEvent) = {
    val text = evt.srcElement.textContent
    classes.find(n => n.displayName == text) match {
      case None => g.alert("Not found")
      case Some(n) => g.alert("Methods called: " + n.asInstanceOf[MethodNode].methodsCalled.mkString(";"))
    }
  }

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

}
