package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.svg.Line
import org.singlespaced.d3js.{DragEvent, Link, d3, forceModule}

import scala.scalajs.js.JSConverters.genTravConvertible2JSRichGenTrav
import scala.scalajs.js.{Array, Dynamic, DynamicImplicits}


final case class GraphNode(displayName: String, group: Int, data: Node) extends forceModule.Node

final case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

/**
  * A D3JS class modeling our graph.
  * Each node has a name, and a group, as well as data, which points to the actual Utils.Node containing the
  * relevant information.
  */
object D3Graph {

  private var callGraph: CallGraph = CallGraph(Set.empty)

  def setCallGraph(callGraph: CallGraph): Unit = this.callGraph = callGraph

  def getCallGraph: CallGraph = callGraph

  // For dynamic operations (when the binder (d3js) doesn't know)
  val d3d = Dynamic.global.d3
  val color = d3.scale.category10()
  val width = 800.0
  val height = 600.0

  var selectedNode: Option[GraphNode] = None

  // Init svg
  val svg = d3.select("#main")
    .append("svg")
    .attr("width", width)
    .attr("height", height)
    .append("svg:g")
    .on("click", (e: dom.EventTarget) => ContextMenu.hide())
    .call(d3.behavior.zoom().on("zoom", rescale _))

  svg.append("svg:rect") // To be able to pan and zoom from blank space
    .attr("width", width)
    .attr("height", height)
    .attr("fill", "transparent")

  // rescale g
  def rescale(d: dom.EventTarget, i: Double): Unit = {
    val trans = d3d.event.translate
    val scale = d3d.event.scale
    vis.attr("transform", "translate(" + trans + ")" + " scale(" + scale + ")")
  }

  // Arrow style for links
  svg.append("svg:defs").selectAll("marker")
    .data(Array("end"))
    .enter().append("svg:marker")
    .attr("id", "end")
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 15)
    .attr("refY", 0)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5")

  val vis = svg.append("svg:g")

  // Init force layout
  val force = d3.layout.force[GraphNode, GraphLink]()
    .size((width, height))
    .charge(-400)
    .linkDistance(100)

  // Layer code
  def layer = Layers.current()

  // Transform a line to a Path
  def line: Line[GraphNode] = d3.svg.line()
    .x((d: GraphNode) => d.x.fold(0.0)(identity))
    .y((d: GraphNode) => d.y.fold(0.0)(identity))

  // Convert a Link into a Path
  def lineData: (GraphLink) => String =
    (d: GraphLink) => line(Array(d.source, d.target))

  // Draging nodes
  val node_drag = d3.behavior.drag[GraphNode]()
    .on("dragstart", (d: GraphNode, _: Double) => {
      d3d.event.sourceEvent.stopPropagation()
      force.stop()
    })
    .on("drag", (d: GraphNode, i: Double) => {
      val event = d3.event.asInstanceOf[DragEvent]
      d.px = d.px.fold(0.0)(_ + event.dx)
      d.py = d.py.fold(0.0)(_ + event.dy)
      d.x = d.x.fold(0.0)(_ + event.dx)
      d.y = d.y.fold(0.0)(_ + event.dy)
      tick(null)
    })
    .on("dragend", (d: GraphNode, _: Double) => {
      d.fixed = d.fixed.fold(1.0)(_ => 1.0)
      force.resume()
    })

  // Motion of the elements
  def tick(e: dom.Event): Unit = {
    vis.selectAll[GraphLink](".link")
      .attr("d", lineData)
    vis.selectAll[GraphNode]("text")
      .attr("x", (d: GraphNode) => d.x)
      .attr("y", (d: GraphNode) => d.y)
    vis.selectAll[GraphNode]("circle")
      .attr("cx", (d: GraphNode) => d.x)
      .attr("cy", (d: GraphNode) => d.y)
  }

  def contextMenu(n: GraphNode): Unit = {
    selectedNode = Some(n)
    val mouseEvent = d3.event.asInstanceOf[dom.MouseEvent]
    ContextMenu.show(mouseEvent.clientX, mouseEvent.clientY)
    mouseEvent.preventDefault()
  }

  def update(): Unit = {
    var link = vis.selectAll[GraphLink](".link").data[GraphLink](Array[GraphLink]())
    var node = vis.selectAll[GraphNode](".node").data[GraphNode](Array[GraphNode]())
    val nodes = layer.nodes.toJSArray
    val links = layer.links.toJSArray

    link.exit().remove()
    link = link.data(links)
    link.enter().insert("path", ".node")
      .attr("class", "link")
      .attr("marker-end", "url(#end)")

    // NB: the function arg is crucial here! nodes are not known by index!
    node.exit().remove()
    node = node.data(nodes, (d: GraphNode) => d.data match {
      case mn: MethodNode => mn.className + mn.encodedName
      case cn: ClassNode => cn.encodedName
    })
    node.enter().append("g")
      .attr("class", "node")
      .on("click", click _) // TODO: change for something like dblclick
      .on("contextmenu", contextMenu _)
      .call(node_drag)
    node.append("circle")
      .attr("r", 5)
      .attr("fill", (d: GraphNode) => color(d.group.toString))
    node.append("text")
      .attr("dx", 10)
      .attr("dy", ".35em")
      .text((n: GraphNode) => n.displayName)

    force
      .nodes(nodes)
      .links(links)
      .on("tick", tick _)
      .start()
  }

  def click(n: GraphNode): Unit = {
    if (d3d.event == null || !DynamicImplicits.truthValue(d3d.event.defaultPrevented)) {
      //TODO: review this part and the addMethods to not add already existing nodes
      n.data match {
        case method: MethodNode =>
          val methods = method.methodsCalled
          layer.addMethods(n, methods)
        case cl: ClassNode =>
        //TODO: add the click on class node
      }
      update()
    }
  }

  def renderGraph(): Unit = {
    for (n <- callGraph.classes.filter(_.isExported)) {
      val node = GraphNode(Decoder.decodeClass(n.encodedName), 0, n)
      layer.nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(Decoder.decodeMethod(n.encodedName, m.encodedName), 1, m)
        layer.nodes += target
        layer.links += GraphLink(node, target)
      }
    }
    update()
  }

  /**
    * Context menu callbacks
    */
  ContextMenu.setNewLayerCallback((e: dom.Event) => {
    Layers.addLayer()
    layer.nodes += D3Graph.selectedNode.get // TODO: check if really defined
    update()
    Visualization.showLayers()
    ContextMenu.hide()
  })

  ContextMenu.setExpandCallback((e: dom.Event) => selectedNode match {
    case Some(node) =>
      click(node)
      selectedNode = None
      ContextMenu.hide()
    case _ => sys.error("Should not happen")
  })

  ContextMenu.setHideCallback((e: dom.Event) => selectedNode match {
    case Some(node) =>
      layer.nodes -= node
      layer.links --= layer.links.filter(l => l.source == node || l.target == node)
      selectedNode = None
      update()
      ContextMenu.hide()
    case _ => sys.error("Should not happen")
  })


}