package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils._
import org.scalajs.dom._
import org.scalajs.dom.raw.FileReader
import upickle.default._
import scala.collection._
import scala.scalajs.js.Dynamic.{global => g}
import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Visualization extends JSApp {
  var graph = Seq[Utils.Node]()

  val box = input(`type` := "text", placeholder := "Type here !").render
  val output = div().render

  box.onkeyup = render _

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
      graph = upickle.default.read[Seq[Utils.Node]](text).sortBy(_.encodedName)
      val target = document.getElementById("list")
      target.appendChild(div(div(box), output).render)
      render(evt)
    }
  }

  def renderList = ul(
    for {
      node <- graph
      if node.encodedName.toLowerCase.startsWith(box.value.toLowerCase)
    } yield li(node.encodedName)
  ).render

  def render(e: Event) = {
    output.innerHTML = ""
    output.appendChild(renderList)
  }
}
