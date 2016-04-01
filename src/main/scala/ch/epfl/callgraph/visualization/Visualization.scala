package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{MethodNode, ClassNode}

import org.scalajs.dom.window
import ch.epfl.callgraph.utils._
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.dom.{Event, MouseEvent, UIEvent, console, document}
import upickle.default._
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  val classes = collection.mutable.Set[ClassNode]()
  val methods = collection.mutable.Set[MethodNode]()
//  var graph = Seq[Utils.Node]()

  val fileInput = input(`type` := "file").render
  val box = input(`type` := "text", placeholder := "Type here to search !").render
  val exported = input(`type` := "checkbox", checked).render
  val searchField = div(box, " Only exported:", exported).render
  val output = span.render

  box.onkeyup = searchList _
  exported.onclick = searchList _

  def main(): Unit = {
    val width = window.innerWidth
    val height = window.innerHeight
    val target = document.getElementById("nav").asInstanceOf[Div]
    target.appendChild(fileInput)
    fileInput.onchange = readFile(target) _
  }

//  def filter(cond: Utils.Node => Boolean): Seq[Utils.Node] = graph.filter(cond)

  def readFile(target: Div)(evt: Event) = {
    evt.stopPropagation()
    target.innerHTML = ""
    target.appendChild(div(searchField, output).render)
    val reader = new FileReader()
    reader.readAsText(fileInput.files(0))
    reader.onload = (e: UIEvent) => {
      val text = reader.result.asInstanceOf[String]
      for (node <- upickle.default.read[Seq[Utils.Node]](text)) {
        node match {
          case node: MethodNode => methods += node
          case node: ClassNode => classes += node
        }
      }
      searchList(e)
      D3Graph.renderGraph(classes.filter(_.isExported).toSeq)
    }
  }

  def renderList = ul(
    for {
      node <- classes.toSeq
      if (if (exported.checked) node.isExported else true) &&
        node.displayName.toLowerCase.contains(box.value.toLowerCase)
    } yield li(node.displayName, onclick := view _)
  ).render

  def view(e: MouseEvent) = {
    val text = e.srcElement.textContent
    classes.find(n => n.displayName == text) match {
      case None => g.alert("Not found")
      case Some(n) => g.alert("Methods called: " + n.asInstanceOf[MethodNode].methodsCalled.mkString(";"))
    }
  }

  def searchList(e: Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

  def onlyExported(node: Utils.Node) =
    if (exported.checked) node.isExported else true

}
