package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._
import org.singlespaced.d3js.DragEvent
import org.singlespaced.d3js.scale.Ordinal
import org.singlespaced.d3js.selection.Update
import org.singlespaced.d3js.svg.Line

import scala.collection._
import scala.scalajs.js
import scala.scalajs.js.{Array, Dynamic}
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

  val d3d: Dynamic = js.Dynamic.global.d3
  val color: Ordinal[String, String] = d3.scale.category10()
  val width: Double = 800
  val height: Double = 600

  var selectedNode: GraphNode = null

  // Init svg
  val outer = d3.select("#main")
    .append("svg:svg")
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

  val inner = outer
    .append("svg:g")
    .call(d3.behavior.zoom().on("zoom", rescale _))
    .on("dblclick.zoom", null)
    .append("svg:g")
    .on("mousedown", mouseDown _)

//  vis.append("svg:rect")
//    .attr("width", width)
//    .attr("height", height)
//    .attr("fill", "white")

  // Init force layout
  val force = d3.layout.force[GraphNode, GraphLink]()
    .size((width, height))
    .charge(-400)
    .linkDistance(100)
    .on("tick", tick _)

  // rescale g
  def rescale(d: dom.EventTarget, i: Double): Unit = {
    if (selectedNode == null) {
      val trans = d3d.event.translate
      val scale = d3d.event.scale
      inner.attr("transform", "translate(" + trans + ")" + " scale(" + scale + ")")
    }
  }

  // Mouse event
  def mouseDown(d: dom.EventTarget) {
    ContextMenu.hide()
    if (selectedNode == null) {
      // allow panning if nothing is selected
      inner.call(d3.behavior.zoom().on("zoom"), rescale _)
    }
  }

  // Layer code
  def layer = layers.current

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
      force.stop()
      selectedNode = d
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
      tick(null)
      //      selectedNode = null // otherwise the selected node gets reset to null
      force.resume()
    })

  // Layout properties
  val links: Array[GraphLink] = force.links()
  val nodes: Array[GraphNode] = force.nodes()
  var link: Update[GraphLink] =
    inner.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
  var node: Update[GraphNode] =
    inner.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())
  var text: Update[GraphNode] =
    inner.selectAll[GraphNode]("text.label").data[GraphNode](js.Array[GraphNode]())

  // Motion of the elements
  def tick(e: dom.Event): Unit = {
    text
      .attr("transform", (d: GraphNode) => "translate(" + d.x + "," + d.y + ")")
    link
      .attr("d", lineData)
    node
      .attr("cx", (d: GraphNode) => d.x)
      .attr("cy", (d: GraphNode) => d.y)
  }

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
    def subClasses(root: ClassNode) =
      callGraph.classes.filter(_.superClass == Some(root.encodedName))

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
      nodes += newNode
      links += GraphLink(source, newNode)
    }

    /**
      * Add a link from source to methodName in node to the graph
      *
      * @param node
      * @param methodName
      */
    def addLinkToGraph(node: ClassNode, methodName: String) = {
      nodes.find(_.data.encodedName == methodName) match {
        // Find the node in the graph
        case Some(graphNode) => links += GraphLink(source, graphNode)
        case None => addMethodNode(methodName, node)
      }

      update()
    }

    for ((c, ms) <- methods) {
      callGraph.classes.find(_.encodedName == c) match {
        case Some(classNode) => ms.foreach(addLinkToGraph(classNode, _))
        case _ => dom.console.log("no class found " + c)
      }
    }
  }

  def click(n: GraphNode): Unit = {
    selectedNode = n
    //    if (!d3.event.asInstanceOf[dom.Event].defaultPrevented) {
    //TODO: review this part and the addMethods to not add already existing nodes
    n.data match {
      case method: MethodNode =>
        val methods = method.methodsCalled
        addMethods(n, methods)
        update()
      case cl: ClassNode =>
    }
    //      dom.console.log("Clicked on: " + n.name)
    //    }
  }

  def contextMenu(n: GraphNode): Unit = {
    selectedNode = n
    val mouseEvent = d3.event.asInstanceOf[dom.MouseEvent]
    ContextMenu.show(mouseEvent.clientX, mouseEvent.clientY)
    mouseEvent.preventDefault()
  }

  def update(): Unit = {

    link = link.data(links)
    link.enter().insert("path", ".node")
      .attr("class", "link")
      .attr("d", lineData)
      .attr("marker-end", "url(#end)")
    link.exit().remove()

    node = node.data(nodes)
    node.enter().append("circle")
      .attr("class", "node")
      .attr("cx", (d: GraphNode) => d.x)
      .attr("cy", (d: GraphNode) => d.y)
      .attr("r", 5)
      .attr("fill", (d: GraphNode) => color(d.group.toString))
      .on("click", click _)
      .on("contextmenu", contextMenu _)
      .call(node_drag)
    node.exit().remove()

    text = text.data(nodes)
    text.enter().append("text")
      .attr("class", "label")
      .attr("dx", 12)
      .attr("dy", ".35em")
      .text((n: GraphNode) => n.name)
    text.exit().remove()

    if (d3.event != null) {
      // prevent browser's default behavior
      d3.event.asInstanceOf[dom.Event].preventDefault()
    }

    force.start()
  }

  def spliceLinksForNode(node: GraphNode): Unit = {
    val toSplice = links.filter(l => l.source == node || l.target == node)
    toSplice.map(l => links.splice(links.indexOf(l), 1))
  }

  ContextMenu.setExpandCallback((e: dom.Event) => {
    if (selectedNode != null) click(selectedNode)
    dom.console.log(s"Expanding node: $selectedNode")
    ContextMenu.hide()
  })

  ContextMenu.setHideCallback((e: dom.Event) => {
    if (selectedNode != null) {
      nodes.splice(nodes.indexOf(selectedNode), 1)
      spliceLinksForNode(selectedNode)
    }
    update()
    dom.console.log(s"Hiding node: $selectedNode")
    selectedNode = null
    ContextMenu.hide()
  })

  def renderGraph(): Unit = {
    for (n <- callGraph.classes.filter(_.isExported)) {
      val node = GraphNode(n.displayName, 0, n)
      force.nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(n.displayName + "." + m.displayName, 1, m)
        force.nodes += target
        force.links += GraphLink(node, target)
      }
    }
    update()
  }

}