package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{FileReader, HTMLLIElement}
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

  def loop(cn: String, mn: String, target: GraphNode): Unit = {
    val classes = D3Graph.getCallGraph.classes
    target.data match {
      case mi: MethodNode =>
        for ((cc, mcs) <- mi.calledFrom; mc <- mcs) {
          val mn = classes.find(_.encodedName == cc).flatMap(_.methods.find(_.encodedName == mc))
          if (mn.isDefined) {
            val node = GraphNode(Decoder.decodeMethod(cc, mc), 1, mn.get)
            if (Layers.current().nodes.add(node)) {
              val link = GraphLink(node, target)
              Layers.current().links.add(link)
              loop(cc, mc, node)
            }
          }
        }
      case ci: ClassNode => // TODO: à voir quoi faire !!
    }
    D3Graph.update()
  }

  def view(encodedName: String) = (e: sdom.MouseEvent) => {
    //    val text = e.target.valueOf().asInstanceOf[HTMLLIElement].innerHTML
    // TODO: code to review and improve....!!!!
    val as = encodedName.split('.')
    if (as.length == 2) {
      val className = as(0)
      val methodName = as(1)
      val node = D3Graph.getCallGraph.classes.find(_.encodedName == className).get
      val mNode = node.methods.find(_.encodedName == methodName).get
      val classNode = GraphNode(Decoder.decodeMethod(className, methodName), 1, mNode)
      Layers.addLayer()
      Layers.current().nodes += classNode
      loop(className, methodName, classNode)
      D3Graph.update()
      Visualization.showLayers()
    } else if (as.length == 1) {

      // TODO: Expand node!!!
    } else {
      g.alert("Should not come here!!!")
    }
    //    D3Graph.getCallGraph.classes.find(n => n.encodedName == encodedName) match {
    //      case None => g.alert("Not found")
    //      case Some(n) => g.alert(s"Found: ${n.encodedName}")
    //    }
  }

  def renderList = {
    def exp(node: Node): Boolean = !exported.checked || node.isExported
    val list = if (methods.checked)
      for (c <- D3Graph.getCallGraph.classes.toSeq; m <- c.methods; if exp(m))
        yield (Decoder.decodeMethod(c.encodedName, m.encodedName), c.encodedName + "." + m.encodedName)
      else for (c <- D3Graph.getCallGraph.classes.toSeq; if exp(c))
        yield (Decoder.decodeClass(c.encodedName), c.encodedName)

    val search = box.value.toLowerCase
    ul(for {
      (s, h) <- list
      s1 = if (search.contains(".")) search.toLowerCase.split('.') else Array(search.toLowerCase)
      if s.toLowerCase.contains(s1(0)) && s.toLowerCase.contains(s1(s1.length - 1))
    } yield li(s, onclick := view(h))).render
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
