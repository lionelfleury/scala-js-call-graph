package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, Node}
import org.scalajs.{dom => sdom}
import sdom.html.Div
import sdom.raw.{FileReader, HTMLLIElement}
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

  box.onkeyup = searchList _
  exported.onclick = searchList _
  methods.onclick = searchList _

  def main(): Unit = {
    val target = sdom.document.getElementById("nav").asInstanceOf[Div]
    target.appendChild(fileInput)
    fileInput.onchange = readFile(target) _
  }


  def updateHtmlAfterLoad(target: Div) = {
    target.innerHTML = ""
    target.appendChild(div(searchField, output, layersHTML, ContextMenu.nav).render)
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
      D3Graph.setCallGraph(callGraph)
      D3Graph.renderGraph()
      showLayers()
    }
  }

  def view = (e: sdom.MouseEvent) => {
    val text = e.target.valueOf().asInstanceOf[HTMLLIElement].innerHTML
    D3Graph.getCallGraph().classes.find(n => Decoder.decodeClass(n.encodedName) == text) match {
      case None => g.alert("Not found")
      case Some(n) => g.alert(s"Found: ${n.encodedName}")
    }
  }

  def renderList = {
    def exp(node: Node): Boolean = !exported.checked || node.isExported
    val list = methods.checked match {
      case true => for (c <- D3Graph.getCallGraph().classes.toSeq; m <- c.methods; if exp(m)) yield Decoder.decodeMethod(c.encodedName, m.encodedName)
      case _ => for (c <- D3Graph.getCallGraph().classes.toSeq; if exp(c)) yield Decoder.decodeClass(c.encodedName)
    }
    val search = box.value.toLowerCase
    ul(
      for {
        s <- list
        s1 = if (search.contains(".")) search.toLowerCase.split('.') else Array(search.toLowerCase)
        if s.toLowerCase.contains(s1(0)) && s.toLowerCase.contains(s1(s1.length - 1))
      } yield li(s, onclick := view)
    ).render
  }

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

  def showLayers(): Unit = {
    layersHTML.innerHTML = ""
    layersHTML.appendChild(Layers.toHTMLList)
  }

  /*
      Context menu callbacks
   */
  ContextMenu.setNewLayerCallback((e: sdom.Event) => {
    Layers.addLayer()
    Layers.last().nodes += D3Graph.selectedNode.get // TODO: check if really defined
    D3Graph.update()
    showLayers()
    ContextMenu.hide()
  })

}
