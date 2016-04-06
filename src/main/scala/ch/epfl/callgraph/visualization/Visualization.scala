package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{ClassNode, MethodNode, CallGraph}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  var callGraph: CallGraph = null

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
      callGraph = upickle.read[CallGraph](text)
      sdom.console.log("Classes:" + callGraph.classes.size)

      searchList(e)
      D3Graph.renderGraph(callGraph)
    }
  }

  def view(evt: sdom.MouseEvent) = {
    val text = evt.srcElement.textContent
    callGraph.classes.find(n => n.displayName == text) match {
      case None => g.alert("Not found")
      case Some(n) => g.alert("Methods called: " + n.asInstanceOf[MethodNode].methodsCalled.mkString(";"))
    }
  }

  def renderList = ul(
    for {
      node <- callGraph.classes.toSeq
      if (if (exported.checked) node.isExported else true) &&
        node.displayName.toLowerCase.contains(box.value.toLowerCase)
    } yield li(node.displayName, onclick := view _)
  ).render

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

}
