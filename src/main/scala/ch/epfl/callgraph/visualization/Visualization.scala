package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.CallGraph
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  val fileInput = input(`type` := "file").render
  val box = input(`type` := "text", placeholder := "Type here to search !").render
  val exported = input(`type` := "checkbox", checked).render
  val methods = input(`type` := "checkbox").render
  val output = span.render
  val layersHTML = div(`class` := "layers").render

  val searchField = div(box, div(" Only exported:", exported), div(" Methods:", methods)).render

  box.onkeyup = (e: KeyboardEvent) => if (e.keyCode == 13) searchList(e)
  exported.onclick = searchList _
  methods.onclick = searchList _

  def main(): Unit = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.appendChild(fileInput)
    fileInput.onchange = readFile(target) _
  }


  def updateHtmlAfterLoad(target: Div) = {
    target.innerHTML = ""
    target.appendChild(div(searchField, layersHTML, output, ContextMenu.nav).render)
  }

  def readFile(target: Div)(evt: sdom.Event) = {
    evt.stopPropagation()
    updateHtmlAfterLoad(target)
    val reader = new FileReader()
    reader.readAsText(fileInput.files(0))
    reader.onload = (e: sdom.UIEvent) => {
      val text = reader.result.asInstanceOf[String]
      val callGraph = upickle.read[CallGraph](text)
      searchList(e)
      D3GraphController.init(Layers.current().data, callGraph)
      D3Graph.update()
      showLayers()
    }
  }

  def view(encodedName: String) = (e: sdom.MouseEvent) => Layers.openNode(encodedName)

  def renderList = {
    val search = box.value.toLowerCase
    val Array(className, methodName) = if (search.contains(" ")) search.split(' ').take(2) else Array(search, "")
    val result = D3GraphController.search(className, methodName, exported.checked).toSeq
    ul(result map { case (displayName, encodedName) => li(displayName, onclick := view(encodedName)) }).render
  }

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

  def showLayers(): Unit = {
    layersHTML.innerHTML = ""
    layersHTML.appendChild(Layers.toHTMLList)
  }

}
