package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom
import org.scalajs.dom.{EventTarget, MouseEvent}
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._
import org.singlespaced.d3js.DragEvent

import scala.collection._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object D3Graph {

  case class GraphNode(name: String, group: Int, data: Node) extends forceModule.Node

  case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

}

/**
  * A D3JS class modeling our graph.
  * Each node has a name, and a group, as well as data, which points to the actual Utils.Node containing the
  * relevant information.
  */
class D3Graph(callGraph: CallGraph, layers: Layers) {

  import D3Graph._

  val d3d = js.Dynamic.global.d3
  val color = d3.scale.category10()
  val width: Double = 800
  val height: Double = 800

  var selectedNode: GraphNode = null

  // Init svg
  val outer = d3.select("#main").append("svg:svg")
    .attr("width", width)
    .attr("height", height)
    .attr("pointer-events", "all")

  // Arrow style for links
  outer.append("svg:defs").selectAll("marker")
    .data(js.Array("end"))
    .enter().append("svg:marker")
    .attr("id", "end")
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 15)
    .attr("refY", -1.5)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5")

  // rescale g
  def rescale(d: dom.EventTarget, i: Double): Unit = {
    if (selectedNode == null) {
      val trans = d3d.event.translate
      val scale = d3d.event.scale
      vis.attr("transform", "translate(" + trans + ")" + " scale(" + scale + ")")
    }
  }

  val vis = outer
    .append("svg:g")
    .call(d3.behavior.zoom().on("zoom", rescale _))
    .on("dblclick.zoom", null)
    .append("svg:g")
    .on("mousedown", mouseDown _)

  vis.append("svg:rect")
    .attr("width", width)
    .attr("height", height)
    .attr("fill", "white")

  def mouseDown(d: dom.EventTarget) {
    ContextMenu.hide
    if (selectedNode == null) {
      // allow panning if nothing is selected
      vis.call(d3.behavior.zoom().on("zoom"), rescale _)
    }
  }

  /**
    * Layer related code
    */
  def layer = layers.current

  /* Transform a line to a Path */
  def line = d3.svg.line()
    .x((d: GraphNode) => d.x.get)
    .y((d: GraphNode) => d.y.get)

  /** Convert a Link into a Path */
  def lineData = (d: GraphLink) => line(js.Array(d.source, d.target))


  var link = vis.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
  var node = vis.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())
  var text = vis.selectAll[GraphNode]("text.label").data[GraphNode](js.Array[GraphNode]())

  def tick(e: dom.Event): Unit = {
    text
      .attr("transform", (d: GraphNode) => "translate(" + d.x + "," + d.y + ")")
    link
      .attr("d", lineData)
    node
      .attr("cx", (d: GraphNode) => d.x)
      .attr("cy", (d: GraphNode) => d.y)
  }

  val force = d3.layout.force[GraphNode, GraphLink]()
    .size((width, height))
    .charge(-400)
    .linkDistance(100)
    .on("tick", tick _)


  val node_drag = d3.behavior.drag[GraphNode]()
    .on("dragstart", dragStart _)
    .on("drag", dragMove _)
    .on("dragend", dragEnd _)


  //d3d.behavior.zoom().scaleExtent(js.Array(0.1, 3.0)).on("zoom", zoom _).asInstanceOf[js.Function]
  //baseSvg.call(zoomListener) //TODO: redefine zoomListener
  def dragStart(d: GraphNode, i: Double) = {
    force.stop() // stops the force auto positioning before you start dragging
    selectedNode = d
  }

  def dragMove(d: GraphNode, i: Double) = {
    val event = d3.event.asInstanceOf[DragEvent]
    d.px = d.px.fold(0.0)(_ + event.dx)
    d.py = d.py.fold(0.0)(_ + event.dy)
    d.x = d.x.fold(0.0)(_ + event.dx)
    d.y = d.y.fold(0.0)(_ + event.dy)
    tick(null) // this is the key to make it work together with updating both px,py,x,y on d !
  }

  def dragEnd(d: GraphNode, i: Double) = {
    d.fixed = d.fixed.fold(1.0)(_ => 1.0) // of course set the node to fixed so the force doesn't include the node in its auto positioning stuff
    selectedNode = null
    force.resume()
    tick(null)
  }

  //  def displayName(meth: MethodNode) = {
  //    val r = new js.RegExp("(.*)\\(.*").exec(meth.displayName)
  //    meth.className + "." + r(1).get.split('$').last
  //  }

  //  def zoom(d: EventTarget, i: Double): Unit = {
  //    vis.attr("transform", "translate(" + d3d.event.translate + ")scale(" + d3d.event.scale + ")")
  //  }

  /**
    * Add a MethodNode to the graph
    *
    * @param source  the source of the link
    * @param methods all the reachable methods from the source, grouped by className
    */
  def addMethods(source: GraphNode, methods: immutable.Map[String, Seq[String]]) = {

    /**
      * Find all subclasses of a given ClassNode
      *
      * @param root the node we want to find the subclasses of
      * @return a sequence of nodes that have root as parent
      */
    def subClasses(root: ClassNode) = callGraph.classes.filter(_.superClass == Some(root.encodedName))

    /**
      * Add a MethodNode to the graph given its encodedName,
      * This function must not only look in the given class, but also in all its children.
      *
      * @param methodName the name of the method
      * @param root       the class in which we should look the method in
      */
    def addMethodNode(methodName: String, root: ClassNode) = {
      root.methods.find(_.encodedName == methodName) match {
        case Some(methNode) => addNodeToGraph(methNode)
        case None =>
          subClasses(root).foreach(cl => {
            cl.methods.find(_.encodedName == methodName) match {
              case Some(node) => addNodeToGraph(node)
              case None =>
            }
          })
      }
    }

    /**
      * Create a new GraphNode from a MethodNode, and add it to the graph with a link.
      * Link it with the given source
      *
      * @param methNode the MethodNode to convert
      */
    def addNodeToGraph(methNode: MethodNode) = {
      val newNode = GraphNode(methNode.className + "." + methNode.displayName, 5, methNode)
      layer.nodes += newNode
      layer.links += GraphLink(source, newNode)
    }

    /**
      * Add a link from source to methodName in node to the graph
      *
      * @param node
      * @param methodName
      */
    def addLinkToGraph(node: ClassNode, methodName: String) = {
      layer.nodes.find(_.data.encodedName == methodName) match {
        // Find the node in the graph
        case Some(graphNode) => layer.links += GraphLink(source, graphNode)
        case None => addMethodNode(methodName, node)
      }

      update()
    }

    for (c <- methods) {
      callGraph.classes.find(_.encodedName == c._1) match {
        case Some(classNode) => c._2.foreach(addLinkToGraph(classNode, _))
        case _ => dom.console.log("no class found " + c._1)
      }
    }
  }

  def click(n: GraphNode) = {
    if (!d3.event.asInstanceOf[dom.Event].defaultPrevented) {
      n.data match {
        case method: MethodNode =>
          val methods = method.methodsCalled
          addMethods(n, methods)
          update()
        case cl: ClassNode => Nil
      }
      dom.console.log("Clicked on: " + n.name)
    }
  }

  def contextMenu(n: GraphNode) = {
    selectedNode = n
    val mouseEvent = d3.event.asInstanceOf[dom.MouseEvent]
    ContextMenu.show(mouseEvent.clientX, mouseEvent.clientY)
    mouseEvent.preventDefault()
  }

  def update() {
    val ns = layer.nodes.toJSArray
    val ls = layer.links.toJSArray

    force
      .nodes(ns)
      .links(ls)
      .start()

    link = link.data(ls) //TODO: manque un bout
    link.exit().remove()
    link.enter().insert("path", ".node")
      .attr("class", "link")
      .attr("d", lineData)
      .attr("marker-end", "url(#end)")

    node = node.data(ns)
    node.exit().remove()
    node.enter().append("circle")
      .attr("class", "node")
      .attr("cx", (d: GraphNode) => d.x)
      .attr("cy", (d: GraphNode) => d.y)
      .attr("r", 5)
      .attr("fill", (n: GraphNode) => color(n.group.toString))
      .on("click", click _)
      .on("contextmenu", contextMenu _)
      .call(node_drag)

    text = text.data(ns)
    text.exit().remove()
    text.enter().append("text")
      .attr("class", "label")
      .attr("dx", 12)
      .attr("dy", ".35em")
      .text((n: GraphNode) => n.name)

    if (d3.event != null) {
      // prevent browser's default behavior
      d3.event.asInstanceOf[dom.Event].preventDefault()
    }
  }

  def renderGraph(): Unit = {
    for (n <- callGraph.classes.filter(_.isExported)) {
      val node = GraphNode(n.displayName, 0, n)
      layer.nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(n.displayName + "." + m.displayName, 1, m)
        layer.nodes += target
        layer.links += GraphLink(node, target)
      }
    }
    update()
  }

}