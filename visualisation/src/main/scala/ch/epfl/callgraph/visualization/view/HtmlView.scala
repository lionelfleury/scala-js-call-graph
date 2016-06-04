package ch.epfl.callgraph.visualization.view

import ch.epfl.callgraph.utils.Utils._
import ch.epfl.callgraph.visualization.controller.{D3GraphController, Layers}
import ch.epfl.callgraph.visualization.model.Decoder
import org.scalajs.dom.html.Div
import org.scalajs.dom.{KeyboardEvent, MouseEvent}
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object HtmlView extends JSApp {
  val box = input(`type` := "text", placeholder := "Type here and press enter!").render
  val exported = input(`type` := "checkbox", checked).render
  val reachable = input(`type` := "checkbox", checked).render
  val output = span.render
  val errors = div.render
  val layersHTML = div(`class` := "layers").render

  val searchField = div(box, div(" Only exported:", exported), div(" Only reachable:", reachable)).render

  box.onkeyup = (e: KeyboardEvent) => if (e.keyCode == 13) searchList()
  exported.onclick = (e: MouseEvent) => searchList()
  reachable.onclick = (e: MouseEvent) => searchList()

  def main(): Unit = {
    val target = sdom.document.getElementById("nav")
    target.innerHTML = ""
    target.appendChild(div(searchField, layersHTML, output, errors, ContextMenu.nav).render)
    val text = sdom.document.getElementById("callgraph").innerHTML
    val callGraph = upickle.read[CallGraph](text)
    D3GraphController.init(callGraph)
    searchList()
    showLayers()

    if (callGraph.errors.nonEmpty) {
      showErrors(callGraph)
    }
  }

  def showErrors(callGraph: CallGraph) = {
    errors.innerHTML = ""
    def view(encodedName: String) = (e: sdom.MouseEvent) => D3GraphController.initNewLayer(encodedName)

    val mappedErrors = callGraph.errors groupBy {
      case x: MissingMethodInfo => "methods"
      case x: MissingClassInfo => "classes"
      case _ => "others"
    }

    val methodErrors = mappedErrors.get("methods") match {
      case Some(s) => s collect {
        case x@MissingMethodInfo(encodedName: String, className: String, from: String) =>
          val displayName = Decoder.getDisplayName(x)
          val shortName = Decoder.shortenDisplayName(displayName)
          li(a(shortName, onclick := view(Decoder.getFullEncodedName(className, encodedName))), title := Decoder.fullDisplayName(x))
      }
      case None => Seq()
    }

    val classErrors = mappedErrors.get("classes") match {
      case Some(s) => s collect {
        case x@MissingClassInfo(encodedName: String, from: String) =>
          val displayName = Decoder.getDisplayName(x)
          val shortName = Decoder.shortenDisplayName(displayName)
          li(shortName)
      }
      case None => Seq()
    }

    errors.appendChild(
      if (methodErrors.nonEmpty || classErrors.nonEmpty) div(
        h4("Errors:"),
        if (methodErrors.nonEmpty) div(h5("Missings methods"), ul(methodErrors: _*)) else div(),
        if (classErrors.nonEmpty) div(h5("Missing classes"), ul(classErrors: _*)) else div(),
        id := "errors").render
      else div().render
    )
  }

  def showLeftNav = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.innerHTML = ""
    target.appendChild(div(searchField, layersHTML, output, errors, ContextMenu.nav).render)
  }

  def searchList() = {
    output.innerHTML = ""

    val limit = 25
    val values = box.value.split(' ')
    val result = D3GraphController.search(values, exported.checked, reachable.checked).take(limit + 1)
    val overflow = if (result.size > limit) "more results..." else ""

    def view(encodedName: String) = (e: sdom.MouseEvent) => D3GraphController.initNewLayer(encodedName)

    val list = result.take(limit) map { case (shortName, displayName, encodedName) =>
      li(a(shortName), title := displayName, onclick := view(encodedName))
    }
    val nav = ul(list, i(overflow)).render
    output.appendChild(nav)
  }

  def showLayers(): Unit = {
    layersHTML.innerHTML = ""
    layersHTML.appendChild(Layers.toHTMLList)
  }

}
