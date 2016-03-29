package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils._
import org.scalajs.dom.raw.FileReader
import org.scalajs.dom.{Event, MouseEvent, UIEvent, document}
import upickle.default._

import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  var graph = Seq[Utils.Node]()

  val box = input(`type` := "text", placeholder := "Type here !").render
  val exported = input(`type` := "checkbox", checked).render
  val output = div().render

  box.onkeyup = searchList _
  exported.onclick = searchList _

  def main(): Unit = {
    document.getElementById("fileinput").addEventListener("change", readFile _)
  }

  def readFile(evt: Event) = {
    val file = evt.target.asInstanceOf[org.scalajs.dom.html.Input]
    file.disabled = true
    val reader = new FileReader()
    reader.readAsText(file.files(0))
    reader.onload = (_: UIEvent) => {
      val text = reader.result.asInstanceOf[String]
      graph = upickle.default.read[Seq[Utils.Node]](text).sortBy(_.displayName)
      val target = document.getElementById("list")
      target.appendChild(div(div(box, p("Only exported:", exported)), output).render)
      searchList(evt)
    }
  }

  def renderList = ul(
    for {
      node <- graph
      if (if (exported.checked) node.isExported else true) &&
        node.displayName.toLowerCase.contains(box.value.toLowerCase)
    } yield li(node.displayName, onclick := view _)
  ).render

  def view(e: MouseEvent) = {
    val text = e.srcElement.textContent
    g.alert(text)
  }

  def searchList(e: Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }
}
