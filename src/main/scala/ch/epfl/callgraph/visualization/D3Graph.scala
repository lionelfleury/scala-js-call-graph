package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.ClassNode
import org.scalajs.dom
import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js._
import org.singlespaced.d3js.svg.diagonalModule.Node

import scala.scalajs.js
import scala.scalajs.js.{Array, Function2, Tuple2}

object D3Graph {

  def testGraph(graphNodes: Seq[ClassNode]): Unit = {

    class MyNode extends treeModule.Node {
      var name: String = ""
      var children1: js.Array[treeModule.Node] = new js.Array[treeModule.Node]()
      var _children: js.Array[treeModule.Node] = new js.Array[treeModule.Node]()
      var x1: Double = 0.0
      var y1: Double = 0.0
      var x0: Double = 0.0
      var y0: Double = 0.0
    }

    object MyNode {
      def apply(name: String): MyNode = {
        val n = new MyNode()
        n.name = name
        n
      }
    }

    val width: Double = 800
    val height: Double = 500
    val duration = 750

    val tree = d3.layout.tree[MyNode]().size((height, width))
    val diagonal: Function2[Node, Double, Tuple2[Double, Double]] = d3.svg.diagonal().projection()

    val treeData = js.Array[MyNode]()
    for (n <- graphNodes) {
      val node = MyNode(n.displayName)
      treeData.push(node)
      for (m <- n.methods) {
        val me = MyNode(m)
        node.children1.push(me)
      }
    }

    val svg = d3.select("#main")
      .append("svg")
      .attr("width", width)
      .attr("height", height)
      .append("g")
      .attr("transform", "translate(" + 100 + "," + 20 + ")");

    val root = treeData(0)
    root.x0 = height / 2.0
    root.y0 = 0.0

    update(root)

    // Toggle children on click.
    def click(d: MyNode) {
      if (d.children1.length != 0) {
        d._children = d.children1
        d.children1 = null
      } else {
        d.children1 = d._children
        d._children = null
      }
      update(d)
    }

    def update(source: MyNode): Unit = {
      val nodes = tree.nodes(root).reverse
      val links = tree.links(nodes)

      nodes.foreach(n => n.y = 180.0 * n.depth.getOrElse(1))

      val node = svg.selectAll("g.node").data(nodes)

      // Enter any new nodes at the parent's previous position.
      val nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", (d: MyNode) => "translate(" + d.y0 + "," + d.x0 + ")")
        .on("click", click _)

      nodeEnter.append("circle")
        .attr("r", 1e-6)
        .style("fill", (d: MyNode) => if (d.children1.length != 0) "lightsteelblue" else "#fff")

      nodeEnter.append("text")
        .attr("x", (d: MyNode) => if (d.children1.length != 0) -13 else 13)
        .attr("dy", ".35em")
        .attr("text-anchor", (d: MyNode) => if (d.children1.length != 0) "end" else "start")
        .text((d: MyNode) => d.name)
        .style("fill-opacity", 1e-6)

      // Transition nodes to their new position.
      val nodeUpdate = node.transition()
        .duration(duration)
        .attr("transform", (d: MyNode) => "translate(" + d.y1 + "," + d.x1 + ")")

      nodeUpdate.select("circle")
        .attr("r", 10)
        .style("fill", (d: MyNode) => if (d.children1.length != 0) "lightsteelblue" else "#fff")

      nodeUpdate.select("text")
        .style("fill-opacity", 1)

      // Transition exiting nodes to the parent's new position.
      val nodeExit = node.exit().transition()
        .duration(duration)
        .attr("transform", (d: MyNode) => "translate(" + d.y1 + "," + d.x1 + ")")
        .remove()

      nodeExit.select("circle")
        .attr("r", 1e-6)

      nodeExit.select("text")
        .style("fill-opacity", 1e-6)

      // Update the links…
      val link = svg.selectAll("path.link").data(links)
      //TODO: suppressed a part

      // Enter any new links at the parent's previous position.
      link.enter().insert("path", "g")
        .attr("class", "link")
//        .attr("d", (d: Selection[Link[MyNode]]) =>  {
//          val x = source.x0
//          val y = source.y0
//          diagonal(d)
//        })

      // Transition links to their new position.
      link.transition()
        .duration(duration)
//        .attr("d", diagonal);

      // Transition exiting nodes to the parent's new position.
      link.exit().transition()
        .duration(duration)
//        .attr("d", function(d) {
//          var o = {
//            x: source.x, y: source.y
//          };
//          return diagonal({
//            source: o, target: o
//          });
//        })
        .remove()

      // Stash the old positions for transition.
      nodes.foreach(d => {
        d.x0 = d.x1
        d.y0 = d.y1
      })
    }


  }

  def renderGraph(graphNodes: Seq[ClassNode]): Unit = {

    class MyNode extends forceModule.Node {
      var id: String = ""
    }

    object MyNode {
      def apply(id: String): MyNode = {
        val n = new MyNode()
        n.id = id
        n
      }
    }

    type MyLink = SimpleLink[MyNode]

    object MyLink {
      def apply(x: MyNode, y: MyNode): MyLink = {
        SimpleLink(x, y)
      }
    }

    def click(n: MyNode) = {
      dom.alert("test")
    }

    val width: Double = 800
    val height: Double = 500

    val color = d3.scale.category10()

    val nodes = js.Array[MyNode]()
    val links = js.Array[MyLink]()

    for (n <- graphNodes) {
      val node = MyNode(n.displayName)
      nodes.push(node)
      for (m <- n.methods) {
        val me = MyNode(m)
        nodes.push(me)
        links.push(MyLink(node, me))
      }
    }

    val force = d3.layout.force[MyNode, MyLink]()
      .nodes(nodes)
      .links(links)
      .charge(-120)
      .linkDistance(120)
      .size((width, height)).start()

    val svg = d3.select("#main")
      .append("svg")
      .attr("width", width)
      .attr("height", height)

    val link = svg.selectAll[MyLink](".link")
      .data(links)
      .enter()
      .append("line")
      .attr("class", "link")

    val node = svg.selectAll(".node")
      .data(nodes)
      .enter()
      .append("circle")
      .attr("class", "node")
      .attr("r", 6)
      .on("click", click _)

    val text = svg.selectAll("text.label")
      .data(nodes)
      .enter().append("text")
      .attr("class", "label")
      .attr("dx", 12)
      .attr("dy", ".35em")
      .text((n: MyNode) => n.id)

    def tick(e: dom.Event): Unit = {
      text
        .attr("transform", (d: MyNode) => "translate(" + d.x + "," + d.y + ")")

      node
        .attr("cx", (d: MyNode) => d.x)
        .attr("cy", (d: MyNode) => d.y)

      link
        .attr("x1", (d: MyLink) => d.source.x)
        .attr("y1", (d: MyLink) => d.source.y)
        .attr("x2", (d: MyLink) => d.target.x)
        .attr("y2", (d: MyLink) => d.target.y)
    }

    force.on("tick", tick _)

  }

}