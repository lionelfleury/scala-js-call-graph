package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.visualization.D3Graph.{GraphLink, GraphNode}

import scala.collection.mutable

case class Layer(var name: String, var nodes: mutable.ArrayBuffer[GraphNode], var links: mutable.Set[GraphLink])

class Layers {
  private var selected : Int = 0
  private val layers = collection.mutable.ArrayBuffer[Layer]()

  def addLayer(name: String = "layer" + (selected+1)) : Layer = {
    layers += Layer(name, mutable.ArrayBuffer[GraphNode](), mutable.Set[GraphLink]())
    current
  }

  def current = layers.size match {
    case 0 => addLayer()
    case _ => layers(selected)
  }

  def next = {
    if(selected < layers.size-1)
      selected += 1
    current
  }

  def previous = {
    if(selected > 0) selected -= 1
    current
  }

}
