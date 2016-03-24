package ch.epfl.callgraph.visualization

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.JSApp
import js.Dynamic.{ global => g }
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

object Visualization extends JSApp {

  def main() : Unit = {
    //loadGraph()
    g.alert("Hello World")
  }

  private def loadGraph() = {

    val scalaFun: (js.Any, js.Any) => Unit = (error: js.Any, rawPeople: js.Any) => {
      g.alert(rawPeople)
      Unit
    }
    val jsFun: js.Function2[js.Any, js.Any, Unit] = scalaFun
    d3.json("graph.json", jsFun)
  }

}
