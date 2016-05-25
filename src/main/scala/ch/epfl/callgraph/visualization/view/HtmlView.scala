package ch.epfl.callgraph.visualization.view

import ch.epfl.callgraph.utils.Utils.CallGraph
import ch.epfl.callgraph.visualization.controller.{D3GraphController, Layers}
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object HtmlView extends JSApp {
  val fileInput = input(`type` := "file").render
  val box = input(`type` := "text", placeholder := "Type here and press enter!").render
  val exported = input(`type` := "checkbox", checked).render
  val reachable = input(`type` := "checkbox", checked).render
  val output = span.render
  val layersHTML = div(`class` := "layers").render

  val searchField = div(box, div(" Only exported:", exported), div(" Only reachable:", reachable)).render

  box.onkeyup = (e: KeyboardEvent) => if (e.keyCode == 13) searchList(e)
  exported.onclick = searchList _
  reachable.onclick = searchList _

  def main(): Unit = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.appendChild(fileInput)

    fileInput.onchange = (evt: sdom.Event) => {
      evt.stopImmediatePropagation()
      showLeftNav
      val reader = new FileReader()
      reader.readAsText(fileInput.files(0))
      reader.onload = (e: sdom.UIEvent) => {
        val text = reader.result.asInstanceOf[String]
        val callGraph = upickle.read[CallGraph](text)
        D3GraphController.init(callGraph)
        searchList(e)
        showLayers()
      }
    }
  }

  def showLeftNav = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.innerHTML = ""
    target.appendChild(div(searchField, layersHTML, output, ContextMenu.nav).render)
  }

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""

    val values = box.value.split(' ')
    val result = D3GraphController.search(values, exported.checked, reachable.checked).take(20)

    def view(encodedName: String) = (e: sdom.MouseEvent) => D3GraphController.initNewLayer(encodedName)

    val list = ul(result map {
      case (shortName, displayName, encodedName) =>
        li(a(shortName, style := "cursor: pointer;"), title := displayName, onclick := view(encodedName))
    }).render
    output.appendChild(list)
  }

  def showLayers(): Unit = {
    layersHTML.innerHTML = ""
    layersHTML.appendChild(Layers.toHTMLList)
  }

}
