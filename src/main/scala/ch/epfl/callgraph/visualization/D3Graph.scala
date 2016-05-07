package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._
import org.singlespaced.d3js.DragEvent
import org.singlespaced.d3js.svg.Line

import scala.collection._
import scala.scalajs.js
import js.JSConverters._

/**
  * A D3JS class modeling our graph.
  * Each node has a name, and a group, as well as data, which points to the actual Utils.Node containing the
  * relevant information.
  */
object D3Graph {

  private var callGraph: CallGraph = CallGraph(Set.empty[ClassNode])

  def setCallGraph(callGraph: CallGraph) = this.callGraph = callGraph
  def getCallGraph(): CallGraph = callGraph

  case class GraphNode(displayName: String, group: Int, data: Node) extends forceModule.Node
  case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

  val d3d = js.Dynamic.global.d3
  // For dynamic operations (when the binder (d3js) doesn't know)
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

  // Layer code
  def layer = Layers.current()

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
        case Some(methNode) => addNodeToGraph(root, methNode)
        case None =>
          subClasses(root).foreach(cl => {
            cl.methods.find(_.encodedName == methodName) match {
              case Some(node) => addNodeToGraph(cl, node)
              case None =>
            }
          })
      }
    }

    /**
      * Create a new GraphNode from a MethodNode, and add it to the graph with a link.
      * Link it with the given source
*/
    def addNodeToGraph(classNode: ClassNode, methodNode: MethodNode) = {
      val newNode = GraphNode(Decoder.decodeMethod(classNode.encodedName, methodNode.encodedName), 5, methodNode)
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

    for ((c, ms) <- methods) {
      callGraph.classes.find(_.encodedName == c) match {
        case Some(classNode) => ms.foreach(addLinkToGraph(classNode, _))
        case _ => dom.console.log("no class found " + c)
      }
    }
  }

  def contextMenu(n: GraphNode): Unit = {
    selectedNode = Some(n)
    val mouseEvent = d3.event.asInstanceOf[dom.MouseEvent]
    ContextMenu.show(mouseEvent.clientX, mouseEvent.clientY)
    mouseEvent.preventDefault()
  }

  def update(): Unit = {
    var link = vis.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
    var node = vis.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())
    val nodes = layer.nodes.toJSArray
    val links = layer.links.toJSArray

    link.exit().remove()
    link = link.data(links)
    link.enter().insert("path", ".node")
      .attr("class", "link")
      .attr("marker-end", "url(#end)")

    // NB: the function arg is crucial here! nodes are known by name, not by index!
    node.exit().remove()
    node = node.data(nodes, (d: GraphNode) => d.data.encodedName)
    node.enter().append("g")
      .attr("class", "node")
      .on("click", click _)
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
    if (!js.DynamicImplicits.truthValue(d3d.event.defaultPrevented)) {
      //TODO: review this part and the addMethods to not add already existing nodes
      n.data match {
        case method: MethodNode =>
          val methods = method.methodsCalled
          addMethods(n, methods)
        case cl: ClassNode =>
        //TODO: add the click on class node
      }
      update()
    }
  }

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

}