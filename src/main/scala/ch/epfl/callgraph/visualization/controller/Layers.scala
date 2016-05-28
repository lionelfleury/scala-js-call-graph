package ch.epfl.callgraph.visualization.controller

import ch.epfl.callgraph.utils.Utils.Node
import ch.epfl.callgraph.visualization.model.D3GraphModel.{GraphLink, GraphNode}
import ch.epfl.callgraph.visualization.view.{D3GraphView, HtmlView}
import org.scalajs.dom.raw.HTMLSelectElement

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters.genTravConvertible2JSRichGenTrav
import scala.scalajs.js.ThisFunction
import scalatags.JsDom.all._

final class Layer(val name: String) {
  private val nodes = mutable.HashSet[GraphNode]()
  private val links = mutable.HashSet[GraphLink]()

  def update(): Unit = {
    D3GraphView.update(getNodes, getLinks)
    HtmlView.showLayers()
  }

  def getNodes: js.Array[GraphNode] = nodes.toJSArray

  def getLinks: js.Array[GraphLink] = links.toJSArray

  def addNode(node: Node): GraphNode = {
    val newNode = GraphNode(node)
    nodes.find(_ == newNode) match {
      case Some(n) => n
      case _ =>
        nodes += newNode
        newNode
    }
  }

  def addLink(source: GraphNode, target: GraphNode): Boolean = {
    val newLink = GraphLink(source, target)
    links.add(newLink)
  }

  def removeNode(node: GraphNode) = {
    val newLinks = mutable.HashSet[GraphLink]()
    val newNodes = mutable.HashSet[GraphNode]()
    for (link@GraphLink(src, tgt) <- links if src != node && tgt != node) {
      newLinks += link
      newNodes += src
      newNodes += tgt
    }
    links.clear()
    links ++= newLinks
    nodes.clear()
    nodes ++= newNodes
  }
}

object Layers {
  private var s = 0
  private val layers = mutable.ArrayBuffer[Layer]()

  def toHTMLList = {
    def changeLayer: ThisFunction = (s: HTMLSelectElement) => {
      setCurrent(s.selectedIndex)
      current.update()
    }

    val layerList = for ((layer, index) <- layers.zipWithIndex)
      yield option(value := index, if (s == index) selected else "")(a(layer.name))

    select(layerList, onchange := changeLayer).render
  }

  def current: Layer = layers.size match {
    case 0 => addLayer()
    case _ => layers(s)
  }

  def addLayer(name: String = "layer" + (layers.size + 1)): Layer = {
    layers += new Layer(name)
    last
  }

  private def setCurrent(i: Int): Unit = if (i >= 0 && i < layers.size) s = i

  private def last: Layer = {
    setCurrent(layers.size - 1)
    current
  }
}