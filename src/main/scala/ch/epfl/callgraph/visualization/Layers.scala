package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.visualization.D3Graph.{GraphLink, GraphNode}
import org.scalajs.dom.html.Anchor
import org.scalajs.dom.{Event, MouseEvent}

import scalatags.JsDom.all._
import scala.collection.mutable

case class Layer(name: String) {
  val nodes = mutable.Set[GraphNode]()
  val links = mutable.Set[GraphLink]()
}

class Layers {
  private var selected = 0
  private val layers = mutable.ArrayBuffer[Layer]()

  def addLayer(name: String = "layer" + (layers.size + 1)): Layer = {
    layers += Layer(name)
    current
  }

  def current = layers.size match {
    case 0 => addLayer()
    case _ => layers(selected)
  }

  def next = {
    if (selected < layers.size - 1)
      selected += 1
    current
  }

  def previous = {
    if (selected > 0) selected -= 1
    current
  }

  def setCurrent(i: Int) = {
    if (i != selected && i >= 0 && i < layers.size) {
      selected = i
    }
  }

  def last = {
    setCurrent(layers.size - 1)
    current
  }

  def toHTMLList = ul(
    for (layer <- layers.zipWithIndex) yield {
      val active = if (layer._2 == selected) "active" else "inactive"
      li()(a(href := "", onclick := changeLayer, `class` := active, data.number := layer._2)(layer._1.name))
    }
  ).render

  def changeLayer = (e: MouseEvent) => {
    e.preventDefault()
    val link = e.target.asInstanceOf[Anchor]
    val newLayer = link.getAttribute("data-number").toInt
    setCurrent(newLayer)
  }

}
