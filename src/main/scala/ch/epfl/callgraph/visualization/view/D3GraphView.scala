package ch.epfl.callgraph.visualization.view

import ch.epfl.callgraph.visualization.controller.D3GraphController
import ch.epfl.callgraph.visualization.model.Decoder
import ch.epfl.callgraph.visualization.model.D3GraphModel._
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.svg.Line
import org.singlespaced.d3js.{DragEvent, d3}

import scala.scalajs.js

/**
  * A D3JS class modeling our graph.
  * Each node has a name, and a group, as well as data, which points to the actual Utils.Node containing the
  * relevant information.
  */
class D3GraphView {

  // For dynamic operations (when the binder (d3js) doesn't know)
  val d3d = js.Dynamic.global.d3
  val width = 800.0
  val height = 600.0

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
    .data(js.Array("end"))
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

  // Transform a line to a Path
  def line: Line[GraphNode] = d3.svg.line()
    .x((d: GraphNode) => d.x.fold(0.0)(identity))
    .y((d: GraphNode) => d.y.fold(0.0)(identity))

  // Convert a Link into a Path
  def lineData: (GraphLink) => String =
    (d: GraphLink) => line(js.Array(d.source, d.target))

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

  def update(nodes: js.Array[GraphNode], links: js.Array[GraphLink]): Unit = {
    var link = vis.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
    var node = vis.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())

    // NB: the function arg is crucial here! nodes are not known by index!
    node.exit().remove()
    node = node.data(nodes, (n: GraphNode) => Decoder.getFullEncodedName(n.data))
    node.enter().append("g")
      .attr("class", "node")
      .on("click", click _)
      .on("contextmenu", openContextMenu _)
      .call(node_drag)
    node.append("circle")
      .attr("r", 5)
      .attr("fill", (d: GraphNode) =>
        if (d.data.nonExistent) "gray"
        else if (d.data.isExported) "green"
        else if (d.data.isReachable) "blue"
        else "red")
    node.append("text")
      .attr("dx", 10)
      .attr("dy", ".35em")
      .text((n: GraphNode) => Decoder.shortenDisplayName(n.data))
      .append("title")
      .text((n: GraphNode) => Decoder.getDisplayName(n.data))

    link.exit().remove()
    link = link.data(links)
    link.enter().insert("path", ".node")
      .attr("class", "link")
      .attr("marker-end", "url(#end)")

    force
      .nodes(nodes)
      .links(links)
      .on("tick", tick _)
      .start()
  }

  def reset: Unit = {
    var link = vis.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
    var node = vis.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())

    // NB: the function arg is crucial here! nodes are not known by index!
    node.exit().remove()
    link.exit().remove()
  }

  def click(n: GraphNode): Unit = {
    if (d3d.event == null || !js.DynamicImplicits.truthValue(d3d.event.defaultPrevented))
      D3GraphController.expandAllTo(n)
  }

  var selectedNode: Option[GraphNode] = None

  def openContextMenu(n: GraphNode): Unit = {
    selectedNode = Some(n)
    val mouseEvent = d3.event.asInstanceOf[dom.MouseEvent]
    ContextMenu.show(mouseEvent.clientX, mouseEvent.clientY)
    mouseEvent.preventDefault()
  }

  ContextMenu.setNewLayerCallback((e: dom.Event) => selectedNode foreach { node =>
    D3GraphController.initNewLayer(Decoder.getFullEncodedName(node.data))
    selectedNode = None
    ContextMenu.hide()
  })

  ContextMenu.setExpandCallback((e: dom.Event) => selectedNode foreach { node =>
    D3GraphController.expandAllFrom(node)
    selectedNode = None
    ContextMenu.hide()
  })

  ContextMenu.setHideCallback((e: dom.Event) => selectedNode foreach { node =>
    D3GraphController.hideNode(node)
    selectedNode = None
    ContextMenu.hide()
  })

}
