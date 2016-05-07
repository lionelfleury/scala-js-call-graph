package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.visualization.D3Graph.{GraphLink, GraphNode}
import org.scalajs.dom.MouseEvent

import scala.collection.mutable
import scalatags.JsDom.all._

case class Layer(name: String) {
  val nodes = mutable.Set[GraphNode]()
  val links = mutable.Set[GraphLink]()
}

object Layers {
  private var selected = 0
  private val layers = mutable.ArrayBuffer[Layer]()

  def addLayer(name: String = "layer" + (layers.size + 1)): Layer = {
    layers += Layer(name)
    next()
  }

  def current(): Layer = layers.size match {
    case 0 => addLayer()
    case _ => layers(selected)
  }

  def next(): Layer = {
    if (selected < layers.size - 1)
      selected += 1
    current()
  }

  def previous(): Layer = {
    if (selected > 0) selected -= 1
    current()
  }

  def setCurrent(i: Int): Unit = {
    if (i != selected && i >= 0 && i < layers.size) {
      selected = i
    }
  }

  def last(): Layer = {
    setCurrent(layers.size - 1)
    current()
  }

  def toHTMLList = ul(
    for ((layer, index) <- layers.zipWithIndex; active = if (index == selected) "active" else "inactive") yield
      li()(a(href := "", onclick := changeLayer(index), `class` := active)(layer.name))
  ).render

  def changeLayer(index: Int) = (e: MouseEvent) => {
    e.preventDefault()
    setCurrent(index)
    D3Graph.update()
    Visualization.showLayers()
  }

}
