package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.ClassNode
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._

import scala.scalajs.js
import scala.collection._

object D3Graph {

  def renderGraph(graphNodes: Seq[ClassNode]): Unit = {

  case class GraphNode(name: String, group: Int) extends forceModule.Node
  case class GraphLink(source: GraphNode, target: GraphNode, value: Int) extends Link[GraphNode]
  case class Graph(nodes: IndexedSeq[GraphNode], links: IndexedSeq[GraphLink])

    def click(n: GraphNode) = {
      // TODO : handle the click on nodes
      dom.alert("test")
    }

    val width: Double = 800
    val height: Double = 500

    val color = d3.scale.category10()

    val nodes = js.Array[GraphNode]()
    val links = js.Array[GraphLink]()

    for (n <- graphNodes) {
      val node = GraphNode(n.displayName, 0)
      nodes.push(node)
      for (m <- n.methods) {
        val me = GraphNode(m, 1)
        nodes.push(me)
        links.push(GraphLink(node, me, 1))
      }
    }

    val force = d3.layout.force[GraphNode, GraphLink]()
      .nodes(nodes)
      .links(links)
      .charge(-120)
      .linkDistance(120)
      .size((width, height))
      .start()

    val svg = d3.select("#main")
      .append("svg")
      .attr("width", width)
      .attr("height", height)

    val link = svg.selectAll[GraphLink](".link")
      .data(links)
      .enter()
      .append("line")
      .attr("class", "link")
      .style("stroke", "gray")

    val node = svg.selectAll(".node")
      .data(nodes)
      .enter()
      .append("circle")
      .attr("class", "node")
      .attr("r", 6)
      .attr("fill", (n: GraphNode) => color(n.group.toString))
      .on("click", click _)
      .call(force.drag())

    val text = svg.selectAll("text.label")
      .data(nodes)
      .enter().append("text")
      .attr("class", "label")
      .attr("dx", 12)
      .attr("dy", ".35em")
      .text((n: GraphNode) => n.name)

    def tick(e: dom.Event): Unit = {
      text
        .attr("transform", (d: GraphNode) => "translate(" + d.x + "," + d.y + ")")

      node
        .attr("cx", (d: GraphNode) => d.x)
        .attr("cy", (d: GraphNode) => d.y)

      link
        .attr("x1", (d: GraphLink) => d.source.x)
        .attr("y1", (d: GraphLink) => d.source.y)
        .attr("x2", (d: GraphLink) => d.target.x)
        .attr("y2", (d: GraphLink) => d.target.y)
    }

    force.on("tick", tick _)

  }

}