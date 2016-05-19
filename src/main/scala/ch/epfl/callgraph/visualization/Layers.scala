package ch.epfl.callgraph.visualization

import org.scalajs.dom.raw.HTMLSelectElement

import scala.collection.mutable
import scala.scalajs.js.ThisFunction
import scalatags.JsDom.all._


final case class Layer(name: String) {
  val data = new D3GraphController.Data
}

object Layers {
  private var s = 0
  private val layers = mutable.ArrayBuffer[Layer]()

  def addLayer(name: String = "layer" + (layers.size + 1)): Layer = {
    layers += Layer(name)
    last()
  }

  def current(): Layer = layers.size match {
    case 0 => addLayer()
    case _ => layers(s)
  }

  def openNode(encodedName: String) = {
    addLayer()
    D3GraphController.expandRecursive(current().data, encodedName)
    D3Graph.update()
    Visualization.showLayers()
  }

  private def setCurrent(i: Int): Unit = {
    if (i != s && i >= 0 && i < layers.size) {
      s = i
    }
  }

  private def last(): Layer = {
    setCurrent(layers.size - 1)
    current()
  }

  def toHTMLList = select(
    for ((layer, index) <- layers.zipWithIndex) yield {
      if(s == index)
        option(selected := true, value := index)(a(href := "")(layer.name))
      else // set selected to false does not work in chrome ! Only one option should have the selected tag
        option(value := index)(a(href := "")(layer.name))
    }
  , onchange := changeLayer).render

  private def changeLayer : ThisFunction = (select: HTMLSelectElement) => {
    s = select.selectedIndex
    setCurrent(s)
    D3Graph.update()
    Visualization.showLayers()
  }

}
