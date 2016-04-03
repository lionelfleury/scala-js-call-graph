package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.CallGraph
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._

import scala.collection._
import scala.scalajs.js
import js.JSConverters._

object D3Graph {

  case class GraphNode(name: String,
                       group: Int) extends forceModule.Node

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
      val node = GraphNode(n.encodedName, 0)
      nodes += node
      for (m <- n.methods.toSeq) {
        val target = GraphNode(m, 1)
        nodes += target
        links += GraphLink(node, target)
      }
    }

    update()


    def addMethods(source: GraphNode, methods: Seq[String]) = {
      for (m <- methods) {
        val node = nodes.find(_.name == m).getOrElse(GraphNode(m, 5))
        nodes += node
        links += GraphLink(source, node)
      }
    }

    def click(n: GraphNode) = {
      val ms = callGraph.methods.find(_.encodedName == n.name) match {
        case Some(m) => m.methodsCalled.toSeq
        case _ => Nil
      }
      addMethods(n, ms)
      update()
      dom.console.log("Children count:" + ms.size)
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