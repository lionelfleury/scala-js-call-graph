package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom
import org.scalajs.dom.{EventTarget, MouseEvent}
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._
import org.singlespaced.d3js.DragEvent

import scala.collection._
import scala.collection.immutable.Map
import org.scalajs.dom.html.Div

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._

/**
  * A D3JS class modeling our graph.
  * Each node has a name, and a group, as well as data, which points to the actual Utils.Node containing the
  * relevant information.
  */
object D3Graph {


  case class GraphNode(name: String,
                       group: Int,
                       data: Node) extends forceModule.Node

  case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

  val nodes = mutable.ArrayBuffer[GraphNode]()
  val links = mutable.ArrayBuffer[GraphLink]()



  def renderGraph(callGraph: CallGraph, nav: Div): Unit = {
    val d3d = js.Dynamic.global.d3

    val color = d3.scale.category10()
    val width: Double = 800
    val height: Double = 500

    val force = d3.layout.force[GraphNode, GraphLink]()
      .size((width, height))
      .charge(-400)
      .linkDistance(100)

    val baseSvg: Selection[EventTarget] = d3.select("#main").append("svg")
      .attr("width", width)
      .attr("height", height)

    val svgGroup: Selection[EventTarget] = baseSvg.append("g")

    def zoom(d: EventTarget, i: Double): Unit = {
      svgGroup.attr("transform", "translate(" + d3d.event.translate + ")scale(" + d3d.event.scale + ")")
    }

    val zoomListener: js.Function =
      d3.behavior.zoom[EventTarget]().scaleExtent((0.1, 3.0)).on("zoom", zoom _)
      //d3d.behavior.zoom().scaleExtent(js.Array(0.1, 3.0)).on("zoom", zoom _).asInstanceOf[js.Function]

    //baseSvg.call(zoomListener) //TODO: redefine zoomListener

    var link =
      svgGroup.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
    var node = svgGroup.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())
    var text = svgGroup.selectAll[GraphNode]("text.label").data[GraphNode](js.Array[GraphNode]())

    def tick(): Unit = {
      text
        .attr("transform", (d: GraphNode) => "translate(" + d.x + "," + d.y + ")")

      link
        .attr("x1", (d: GraphLink) => d.source.x)
        .attr("y1", (d: GraphLink) => d.source.y)
        .attr("x2", (d: GraphLink) => d.target.x)
        .attr("y2", (d: GraphLink) => d.target.y)
      node
        .attr("cx", (d: GraphNode) => d.x)
        .attr("cy", (d: GraphNode) => d.y)
    }

    def dragstart(d: GraphNode, i: Double) = {
      force.stop() // stops the force auto positioning before you start dragging
    }

    def dragmove(d: GraphNode, i: Double) = {
      val event = d3.event.asInstanceOf[DragEvent]
      d.px = d.px.fold(0.0)(_ + event.dx)
      d.py = d.py.fold(0.0)(_ + event.dy)
      d.x = d.x.fold(0.0)(_ + event.dx)
      d.y = d.y.fold(0.0)(_ + event.dy)
      tick() // this is the key to make it work together with updating both px,py,x,y on d !
    }

    def dragend(d: GraphNode, i: Double) = {
      d.fixed = d.fixed.fold(1.0)(_ => 1.0) // of course set the node to fixed so the force doesn't include the node in its auto positioning stuff
      tick()
      force.resume()
    }

    val node_drag = d3.behavior.drag[GraphNode]()
      .on("dragstart", dragstart _)
      .on("drag", dragmove _)
      .on("dragend", dragend _)

    for (n <- callGraph.classes.filter(_.isExported)) {
      val node = GraphNode(n.displayName, 0, n)
      nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(m.displayName, 1, m)
        nodes += target
        links += GraphLink(node, target)
      }
    }

    update()



    /**
      * Add a MethodNode to the graph
      * @param source the source of the link
      * @param methods all the reachable methods from the source, grouped by className
      */
    def addMethods(source: GraphNode, methods: Map[String, Seq[String]]) = {

      /**
        * Find all subclasses of a given ClassNode
        * @param root the node we want to find the subclasses of
        * @return a sequence of nodes that have root as parent
        */
      def subClasses(root: ClassNode) = callGraph.classes.filter(_.superClass == Some(root.encodedName))

      /**
        * Add a MethodNode to the graph given its encodedName,
        * This function must not only look in the given class, but also in all its children.
        * @param methodName the name of the method
        * @param root the class in which we should look the method in
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
        * @param methNode the MethodNode to convert
        */
      def addNodeToGraph(methNode: MethodNode) = {
        val newNode = GraphNode(methNode.displayName, 5, methNode)
        nodes += newNode
        links += GraphLink(source, newNode)
      }

      /**
        * Add a link from source to methodName in node to the graph
        * @param node
        * @param methodName
        */
      def addLinkToGraph(node: ClassNode, methodName: String) = {
        nodes.find(_.name == methodName) match { // Find the node in the graph
          case Some(graphNode) => links += GraphLink(source, graphNode)
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
      n.data match {
        case method: MethodNode =>
          val methods = method.methodsCalled
          addMethods(n, methods)
          update()
        case cl : ClassNode => Nil
      }
      dom.console.log("Clicked on: " + n.name)
    }

    def contextMenu(n: GraphNode) = {
      val x = d3.event.asInstanceOf[dom.MouseEvent].clientX + "px"
      val y = d3.event.asInstanceOf[dom.MouseEvent].clientY + "px"
      nav.setAttribute("class", "context-menu--active context-menu")
      nav.setAttribute("style", "left:" +  x + " ;top:" + y)
      d3.event.asInstanceOf[dom.MouseEvent].preventDefault()
    }

    def update() {
      val ns = nodes.toJSArray
      val ls = links.toJSArray

      force
        .nodes(ns)
        .links(ls)
        .on("tick", (_:dom.Event) => tick())
        .start()

      link = link.data(ls) //TODO: manque un bout
      link.exit().remove()
      link.enter().insert("line", ".node")
        .attr("class", "link")
        .attr("x1", (d: Link[GraphNode]) => d.source.x)
        .attr("y1", (d: Link[GraphNode]) => d.source.y)
        .attr("x2", (d: Link[GraphNode]) => d.target.x)
        .attr("y2", (d: Link[GraphNode]) => d.target.y)

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

    }

  }


}