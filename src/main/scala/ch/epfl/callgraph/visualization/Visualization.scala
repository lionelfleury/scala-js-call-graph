package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, Node}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.FileReader
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  var callGraph: CallGraph = null
  var d3Graph: D3Graph = null
  val layers: Layers = new Layers()


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
      callGraph = upickle.read[CallGraph](text)

      searchList(e)
      d3Graph = new D3Graph(callGraph, layers)
      d3Graph.renderGraph()
      showLayers
    }
  }

  //  def view(evt: sdom.MouseEvent) = {
  //    val text = evt.srcElement.textContent
  //    callGraph.classes.find(n => n.displayName == text) match {
  //      case None => g.alert("Not found")
  //      case Some(n) => g.alert("Methods called: " + n.asInstanceOf[MethodNode].methodsCalled.mkString(";"))
  //    }
  //  }

  def renderList = {
    def exp(node: Node) = if (exported.checked) node.isExported else true
    val list = methods.checked match {
      case true => for (c <- callGraph.classes.toSeq; m <- c.methods; if exp(m)) yield Decoder.decodeMethod(c.encodedName, m.encodedName)
      case _ => for (c <- callGraph.classes.toSeq; if exp(c)) yield Decoder.decodeClass(c.encodedName)
    }
    ul(
      for {
        s <- list
        if s.toLowerCase.contains(box.value.toLowerCase)
      } yield li(s)
    ).render
  }

  def searchList(e: sdom.Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }

  def showLayers: Unit = {
    layersHTML.innerHTML = ""
    layersHTML.appendChild(layers.toHTMLList)
  }

  /*
      Context menu callbacks
   */
  ContextMenu.setNewLayerCallback((e: Event) => {
    layers.addLayer()
    layers.last.nodes += d3Graph.selectedNode.fold(sys.error("selected node is not defined!"))(identity)
    d3Graph = new D3Graph(callGraph, layers)
    d3Graph.update()
    showLayers
    ContextMenu.hide()
  })

}
