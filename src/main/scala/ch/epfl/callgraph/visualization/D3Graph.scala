package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._

import scala.collection._
import scala.collection.immutable.Map
import scala.scalajs.js
import js.JSConverters._

object D3Graph {

  case class GraphNode(name: String,
                       group: Int,
                       data: Node) extends forceModule.Node

  case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

  val nodes = mutable.ArrayBuffer[GraphNode]()
  val links = mutable.ArrayBuffer[GraphLink]()

  def renderGraph(callGraph: CallGraph): Unit = {
    val d3d = js.Dynamic.global.d3

    val color = d3.scale.category10()
    val width: Double = 800
    val height: Double = 500

    val force = d3.layout.force[GraphNode, GraphLink]()
      .size((width, height))
      .charge(-400)
      .linkDistance(100)

    val baseSvg = d3.select("#main").append("svg")
      .attr("width", width)
      .attr("height", height)

    val svgGroup = baseSvg.append("g")

    def zoom(): Unit = {
      svgGroup.attr("transform", "translate(" + d3d.event.translate + ")scale(" + d3d.event.scale + ")")
    }

    val zoomListener: js.Function =
      d3d.behavior.zoom().scaleExtent(js.Array(0.1, 3.0)).on("zoom", zoom _).asInstanceOf[js.Function]

    baseSvg.call(zoomListener)



    var link =
      svgGroup.selectAll[GraphLink](".link").data[GraphLink](js.Array[GraphLink]())
    var node = svgGroup.selectAll[GraphNode](".node").data[GraphNode](js.Array[GraphNode]())
    var text = svgGroup.selectAll[GraphNode]("text.label").data[GraphNode](js.Array[GraphNode]())

    def tick(e: dom.Event): Unit = {
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

    for (n <- callGraph.classes.filter(_.isExported)) {
      val node = GraphNode(n.encodedName, 0, n)
      nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(m.encodedName, 1, m)
        nodes += target
        links += GraphLink(node, target)
      }
    }

    update()



    def addMethods(source: GraphNode, methods: Map[String, Seq[String]]) = {

      /* Find all subclasses of a given ClassNode */
      def subClasses(root: ClassNode) = callGraph.classes.filter(_.superClass == Some(root.encodedName))

      /* Find a MethodNode from a encodedName,
       * This function must not only look in the given class, but also
       * in all its children.
       */
      def findMethodNode(methodName: String, root: ClassNode) = {
        root.methods.find(_.encodedName == methodName) match { // Lookup the utils.MethodNode inside the class
          case Some(methNode) => addNodeToGraph(methNode)
          case None => // Need to look in subclasses
            subClasses(root).foreach(cl => {
              cl.methods.find(_.encodedName == methodName) match {
                case Some(node) => addNodeToGraph(node)
                case None =>
              }
            })
        }
      }

      /* Create a ney GraphNode from a MethodNode, and add it to the graph with a link */
      def addNodeToGraph(methNode: MethodNode) = {
        val newNode = GraphNode(methNode.encodedName, 5, methNode)
        nodes += newNode
        links += GraphLink(source, newNode)
      }

      /* Add a link from source to methodName in node to the graph */
      def addLinkToGraph(node: ClassNode, methodName: String) = {
        nodes.find(_.name == methodName) match { // Find the node in the graph
          case Some(graphNode) => links += GraphLink(source, graphNode)
          case None => findMethodNode(methodName, node)

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

    def update() {
      val ns = nodes.toJSArray
      val ls = links.toJSArray

      force
        .nodes(ns)
        .links(ls)
        .on("tick", tick _)
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
        .call(force.drag)

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