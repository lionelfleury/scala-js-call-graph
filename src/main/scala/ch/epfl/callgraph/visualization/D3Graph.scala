package ch.epfl.callgraph.visualization

import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.JSON

object D3Graph {
  /*
    def testGraph(graphNodes: Seq[ClassNode]): Unit = {

      class MyNode extends treeModule.Node {
        var name: String = ""
        override var children = Some(new js.Array[MyNode]())
        var _children = new js.Array[MyNode]()
        override var x = Some(0.0)
        override var y = Some(0.0)
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
          node.children.get.push(me)
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
        if (d.children.nonEmpty) {
          d._children = d.children.get
          d.children = null
        } else {
          d.children = d._children
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
          .style("fill", (d: MyNode) => if (d.children.length != 0) "lightsteelblue" else "#fff")

        nodeEnter.append("text")
          .attr("x", (d: MyNode) => if (d.children.nonEmpty) -13 else 13)
          .attr("dy", ".35em")
          .attr("text-anchor", (d: MyNode) => if (d.children.nonEmpty) "end" else "start")
          .text((d: MyNode) => d.name)
          .style("fill-opacity", 1e-6)

        // Transition nodes to their new position.
        val nodeUpdate = node.transition()
          .duration(duration)
          .attr("transform", (d: MyNode) => "translate(" + d.y + "," + d.x + ")")

        nodeUpdate.select("circle")
          .attr("r", 10)
          .style("fill", (d: MyNode) => if (d.children.nonEmpty) "lightsteelblue" else "#fff")

        nodeUpdate.select("text")
          .style("fill-opacity", 1)

        // Transition exiting nodes to the parent's new position.
        val nodeExit = node.exit().transition()
          .duration(duration)
          .attr("transform", (d: MyNode) => "translate(" + d.y + "," + d.x + ")")
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
          d.x0 = d.x
          d.y0 = d.y
        })
      }


    }
  */
  /*
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
  */
  def testGraph2() = {
    val d3 = js.Dynamic.global.d3
    val width = 800
    val height = 600

    val color = d3.scale.category20()

    val force = d3.layout.force()
      .charge(-120)
      .linkDistance(30)
      .size(js.Array(width, height))

    val svg = d3.select("#main").append("svg")
      .attr("width", width)
      .attr("height", height)

    val graph = JSON.parse(graphJson)

    force
      .nodes(graph.nodes)
      .links(graph.links)
      .start()

    val link = svg.selectAll(".link")
      .data(graph.links)
      .enter().append("line")
      .attr("class", "link")
      .style("stroke", "grey")
      .style("stroke-width", { d: js.Dynamic => math.sqrt(d.value.asInstanceOf[Double]) })

    val node = svg.selectAll(".graphNode")
      .data(graph.nodes)
      .enter().append("circle")
      .attr("class", "node")
      .attr("r", 5)
      .style("fill", { d: js.Dynamic => color(d.group) })
      .call(force.drag)

    node.append("title")
      .text({ d: js.Dynamic => d.name })

    force.on("tick", { () =>
      link.attr("x1", { d: js.Dynamic => d.source.x })
        .attr("y1", { d: js.Dynamic => d.source.y })
        .attr("x2", { d: js.Dynamic => d.target.x })
        .attr("y2", { d: js.Dynamic => d.target.y })

      node.attr("cx", { d: js.Dynamic => d.x })
        .attr("cy", { d: js.Dynamic => d.y })
    })
  }
  val graphJson =
    """
      |{
      |  "nodes":[
      |    {"name":"Myriel","group":1},
      |    {"name":"Napoleon","group":1},
      |    {"name":"Mlle.Baptistine","group":1},
      |    {"name":"Mme.Magloire","group":1},
      |    {"name":"CountessdeLo","group":1},
      |    {"name":"Geborand","group":1},
      |    {"name":"Champtercier","group":1},
      |    {"name":"Cravatte","group":1},
      |    {"name":"Count","group":1},
      |    {"name":"OldMan","group":1},
      |    {"name":"Labarre","group":2},
      |    {"name":"Valjean","group":2},
      |    {"name":"Marguerite","group":3},
      |    {"name":"Mme.deR","group":2},
      |    {"name":"Isabeau","group":2},
      |    {"name":"Gervais","group":2},
      |    {"name":"Tholomyes","group":3},
      |    {"name":"Listolier","group":3},
      |    {"name":"Fameuil","group":3},
      |    {"name":"Blacheville","group":3},
      |    {"name":"Favourite","group":3},
      |    {"name":"Dahlia","group":3},
      |    {"name":"Zephine","group":3},
      |    {"name":"Fantine","group":3},
      |    {"name":"Mme.Thenardier","group":4},
      |    {"name":"Thenardier","group":4},
      |    {"name":"Cosette","group":5},
      |    {"name":"Javert","group":4},
      |    {"name":"Fauchelevent","group":0},
      |    {"name":"Bamatabois","group":2},
      |    {"name":"Perpetue","group":3},
      |    {"name":"Simplice","group":2},
      |    {"name":"Scaufflaire","group":2},
      |    {"name":"Woman1","group":2},
      |    {"name":"Judge","group":2},
      |    {"name":"Champmathieu","group":2},
      |    {"name":"Brevet","group":2},
      |    {"name":"Chenildieu","group":2},
      |    {"name":"Cochepaille","group":2},
      |    {"name":"Pontmercy","group":4},
      |    {"name":"Boulatruelle","group":6},
      |    {"name":"Eponine","group":4},
      |    {"name":"Anzelma","group":4},
      |    {"name":"Woman2","group":5},
      |    {"name":"MotherInnocent","group":0},
      |    {"name":"Gribier","group":0},
      |    {"name":"Jondrette","group":7},
      |    {"name":"Mme.Burgon","group":7},
      |    {"name":"Gavroche","group":8},
      |    {"name":"Gillenormand","group":5},
      |    {"name":"Magnon","group":5},
      |    {"name":"Mlle.Gillenormand","group":5},
      |    {"name":"Mme.Pontmercy","group":5},
      |    {"name":"Mlle.Vaubois","group":5},
      |    {"name":"Lt.Gillenormand","group":5},
      |    {"name":"Marius","group":8},
      |    {"name":"BaronessT","group":5},
      |    {"name":"Mabeuf","group":8},
      |    {"name":"Enjolras","group":8},
      |    {"name":"Combeferre","group":8},
      |    {"name":"Prouvaire","group":8},
      |    {"name":"Feuilly","group":8},
      |    {"name":"Courfeyrac","group":8},
      |    {"name":"Bahorel","group":8},
      |    {"name":"Bossuet","group":8},
      |    {"name":"Joly","group":8},
      |    {"name":"Grantaire","group":8},
      |    {"name":"MotherPlutarch","group":9},
      |    {"name":"Gueulemer","group":4},
      |    {"name":"Babet","group":4},
      |    {"name":"Claquesous","group":4},
      |    {"name":"Montparnasse","group":4},
      |    {"name":"Toussaint","group":5},
      |    {"name":"Child1","group":10},
      |    {"name":"Child2","group":10},
      |    {"name":"Brujon","group":4},
      |    {"name":"Mme.Hucheloup","group":8}
      |  ],
      |  "links":[
      |    {"source":1,"target":0,"value":1},
      |    {"source":2,"target":0,"value":8},
      |    {"source":3,"target":0,"value":10},
      |    {"source":3,"target":2,"value":6},
      |    {"source":4,"target":0,"value":1},
      |    {"source":5,"target":0,"value":1},
      |    {"source":6,"target":0,"value":1},
      |    {"source":7,"target":0,"value":1},
      |    {"source":8,"target":0,"value":2},
      |    {"source":9,"target":0,"value":1},
      |    {"source":11,"target":10,"value":1},
      |    {"source":11,"target":3,"value":3},
      |    {"source":11,"target":2,"value":3},
      |    {"source":11,"target":0,"value":5},
      |    {"source":12,"target":11,"value":1},
      |    {"source":13,"target":11,"value":1},
      |    {"source":14,"target":11,"value":1},
      |    {"source":15,"target":11,"value":1},
      |    {"source":17,"target":16,"value":4},
      |    {"source":18,"target":16,"value":4},
      |    {"source":18,"target":17,"value":4},
      |    {"source":19,"target":16,"value":4},
      |    {"source":19,"target":17,"value":4},
      |    {"source":19,"target":18,"value":4},
      |    {"source":20,"target":16,"value":3},
      |    {"source":20,"target":17,"value":3},
      |    {"source":20,"target":18,"value":3},
      |    {"source":20,"target":19,"value":4},
      |    {"source":21,"target":16,"value":3},
      |    {"source":21,"target":17,"value":3},
      |    {"source":21,"target":18,"value":3},
      |    {"source":21,"target":19,"value":3},
      |    {"source":21,"target":20,"value":5},
      |    {"source":22,"target":16,"value":3},
      |    {"source":22,"target":17,"value":3},
      |    {"source":22,"target":18,"value":3},
      |    {"source":22,"target":19,"value":3},
      |    {"source":22,"target":20,"value":4},
      |    {"source":22,"target":21,"value":4},
      |    {"source":23,"target":16,"value":3},
      |    {"source":23,"target":17,"value":3},
      |    {"source":23,"target":18,"value":3},
      |    {"source":23,"target":19,"value":3},
      |    {"source":23,"target":20,"value":4},
      |    {"source":23,"target":21,"value":4},
      |    {"source":23,"target":22,"value":4},
      |    {"source":23,"target":12,"value":2},
      |    {"source":23,"target":11,"value":9},
      |    {"source":24,"target":23,"value":2},
      |    {"source":24,"target":11,"value":7},
      |    {"source":25,"target":24,"value":13},
      |    {"source":25,"target":23,"value":1},
      |    {"source":25,"target":11,"value":12},
      |    {"source":26,"target":24,"value":4},
      |    {"source":26,"target":11,"value":31},
      |    {"source":26,"target":16,"value":1},
      |    {"source":26,"target":25,"value":1},
      |    {"source":27,"target":11,"value":17},
      |    {"source":27,"target":23,"value":5},
      |    {"source":27,"target":25,"value":5},
      |    {"source":27,"target":24,"value":1},
      |    {"source":27,"target":26,"value":1},
      |    {"source":28,"target":11,"value":8},
      |    {"source":28,"target":27,"value":1},
      |    {"source":29,"target":23,"value":1},
      |    {"source":29,"target":27,"value":1},
      |    {"source":29,"target":11,"value":2},
      |    {"source":30,"target":23,"value":1},
      |    {"source":31,"target":30,"value":2},
      |    {"source":31,"target":11,"value":3},
      |    {"source":31,"target":23,"value":2},
      |    {"source":31,"target":27,"value":1},
      |    {"source":32,"target":11,"value":1},
      |    {"source":33,"target":11,"value":2},
      |    {"source":33,"target":27,"value":1},
      |    {"source":34,"target":11,"value":3},
      |    {"source":34,"target":29,"value":2},
      |    {"source":35,"target":11,"value":3},
      |    {"source":35,"target":34,"value":3},
      |    {"source":35,"target":29,"value":2},
      |    {"source":36,"target":34,"value":2},
      |    {"source":36,"target":35,"value":2},
      |    {"source":36,"target":11,"value":2},
      |    {"source":36,"target":29,"value":1},
      |    {"source":37,"target":34,"value":2},
      |    {"source":37,"target":35,"value":2},
      |    {"source":37,"target":36,"value":2},
      |    {"source":37,"target":11,"value":2},
      |    {"source":37,"target":29,"value":1},
      |    {"source":38,"target":34,"value":2},
      |    {"source":38,"target":35,"value":2},
      |    {"source":38,"target":36,"value":2},
      |    {"source":38,"target":37,"value":2},
      |    {"source":38,"target":11,"value":2},
      |    {"source":38,"target":29,"value":1},
      |    {"source":39,"target":25,"value":1},
      |    {"source":40,"target":25,"value":1},
      |    {"source":41,"target":24,"value":2},
      |    {"source":41,"target":25,"value":3},
      |    {"source":42,"target":41,"value":2},
      |    {"source":42,"target":25,"value":2},
      |    {"source":42,"target":24,"value":1},
      |    {"source":43,"target":11,"value":3},
      |    {"source":43,"target":26,"value":1},
      |    {"source":43,"target":27,"value":1},
      |    {"source":44,"target":28,"value":3},
      |    {"source":44,"target":11,"value":1},
      |    {"source":45,"target":28,"value":2},
      |    {"source":47,"target":46,"value":1},
      |    {"source":48,"target":47,"value":2},
      |    {"source":48,"target":25,"value":1},
      |    {"source":48,"target":27,"value":1},
      |    {"source":48,"target":11,"value":1},
      |    {"source":49,"target":26,"value":3},
      |    {"source":49,"target":11,"value":2},
      |    {"source":50,"target":49,"value":1},
      |    {"source":50,"target":24,"value":1},
      |    {"source":51,"target":49,"value":9},
      |    {"source":51,"target":26,"value":2},
      |    {"source":51,"target":11,"value":2},
      |    {"source":52,"target":51,"value":1},
      |    {"source":52,"target":39,"value":1},
      |    {"source":53,"target":51,"value":1},
      |    {"source":54,"target":51,"value":2},
      |    {"source":54,"target":49,"value":1},
      |    {"source":54,"target":26,"value":1},
      |    {"source":55,"target":51,"value":6},
      |    {"source":55,"target":49,"value":12},
      |    {"source":55,"target":39,"value":1},
      |    {"source":55,"target":54,"value":1},
      |    {"source":55,"target":26,"value":21},
      |    {"source":55,"target":11,"value":19},
      |    {"source":55,"target":16,"value":1},
      |    {"source":55,"target":25,"value":2},
      |    {"source":55,"target":41,"value":5},
      |    {"source":55,"target":48,"value":4},
      |    {"source":56,"target":49,"value":1},
      |    {"source":56,"target":55,"value":1},
      |    {"source":57,"target":55,"value":1},
      |    {"source":57,"target":41,"value":1},
      |    {"source":57,"target":48,"value":1},
      |    {"source":58,"target":55,"value":7},
      |    {"source":58,"target":48,"value":7},
      |    {"source":58,"target":27,"value":6},
      |    {"source":58,"target":57,"value":1},
      |    {"source":58,"target":11,"value":4},
      |    {"source":59,"target":58,"value":15},
      |    {"source":59,"target":55,"value":5},
      |    {"source":59,"target":48,"value":6},
      |    {"source":59,"target":57,"value":2},
      |    {"source":60,"target":48,"value":1},
      |    {"source":60,"target":58,"value":4},
      |    {"source":60,"target":59,"value":2},
      |    {"source":61,"target":48,"value":2},
      |    {"source":61,"target":58,"value":6},
      |    {"source":61,"target":60,"value":2},
      |    {"source":61,"target":59,"value":5},
      |    {"source":61,"target":57,"value":1},
      |    {"source":61,"target":55,"value":1},
      |    {"source":62,"target":55,"value":9},
      |    {"source":62,"target":58,"value":17},
      |    {"source":62,"target":59,"value":13},
      |    {"source":62,"target":48,"value":7},
      |    {"source":62,"target":57,"value":2},
      |    {"source":62,"target":41,"value":1},
      |    {"source":62,"target":61,"value":6},
      |    {"source":62,"target":60,"value":3},
      |    {"source":63,"target":59,"value":5},
      |    {"source":63,"target":48,"value":5},
      |    {"source":63,"target":62,"value":6},
      |    {"source":63,"target":57,"value":2},
      |    {"source":63,"target":58,"value":4},
      |    {"source":63,"target":61,"value":3},
      |    {"source":63,"target":60,"value":2},
      |    {"source":63,"target":55,"value":1},
      |    {"source":64,"target":55,"value":5},
      |    {"source":64,"target":62,"value":12},
      |    {"source":64,"target":48,"value":5},
      |    {"source":64,"target":63,"value":4},
      |    {"source":64,"target":58,"value":10},
      |    {"source":64,"target":61,"value":6},
      |    {"source":64,"target":60,"value":2},
      |    {"source":64,"target":59,"value":9},
      |    {"source":64,"target":57,"value":1},
      |    {"source":64,"target":11,"value":1},
      |    {"source":65,"target":63,"value":5},
      |    {"source":65,"target":64,"value":7},
      |    {"source":65,"target":48,"value":3},
      |    {"source":65,"target":62,"value":5},
      |    {"source":65,"target":58,"value":5},
      |    {"source":65,"target":61,"value":5},
      |    {"source":65,"target":60,"value":2},
      |    {"source":65,"target":59,"value":5},
      |    {"source":65,"target":57,"value":1},
      |    {"source":65,"target":55,"value":2},
      |    {"source":66,"target":64,"value":3},
      |    {"source":66,"target":58,"value":3},
      |    {"source":66,"target":59,"value":1},
      |    {"source":66,"target":62,"value":2},
      |    {"source":66,"target":65,"value":2},
      |    {"source":66,"target":48,"value":1},
      |    {"source":66,"target":63,"value":1},
      |    {"source":66,"target":61,"value":1},
      |    {"source":66,"target":60,"value":1},
      |    {"source":67,"target":57,"value":3},
      |    {"source":68,"target":25,"value":5},
      |    {"source":68,"target":11,"value":1},
      |    {"source":68,"target":24,"value":1},
      |    {"source":68,"target":27,"value":1},
      |    {"source":68,"target":48,"value":1},
      |    {"source":68,"target":41,"value":1},
      |    {"source":69,"target":25,"value":6},
      |    {"source":69,"target":68,"value":6},
      |    {"source":69,"target":11,"value":1},
      |    {"source":69,"target":24,"value":1},
      |    {"source":69,"target":27,"value":2},
      |    {"source":69,"target":48,"value":1},
      |    {"source":69,"target":41,"value":1},
      |    {"source":70,"target":25,"value":4},
      |    {"source":70,"target":69,"value":4},
      |    {"source":70,"target":68,"value":4},
      |    {"source":70,"target":11,"value":1},
      |    {"source":70,"target":24,"value":1},
      |    {"source":70,"target":27,"value":1},
      |    {"source":70,"target":41,"value":1},
      |    {"source":70,"target":58,"value":1},
      |    {"source":71,"target":27,"value":1},
      |    {"source":71,"target":69,"value":2},
      |    {"source":71,"target":68,"value":2},
      |    {"source":71,"target":70,"value":2},
      |    {"source":71,"target":11,"value":1},
      |    {"source":71,"target":48,"value":1},
      |    {"source":71,"target":41,"value":1},
      |    {"source":71,"target":25,"value":1},
      |    {"source":72,"target":26,"value":2},
      |    {"source":72,"target":27,"value":1},
      |    {"source":72,"target":11,"value":1},
      |    {"source":73,"target":48,"value":2},
      |    {"source":74,"target":48,"value":2},
      |    {"source":74,"target":73,"value":3},
      |    {"source":75,"target":69,"value":3},
      |    {"source":75,"target":68,"value":3},
      |    {"source":75,"target":25,"value":3},
      |    {"source":75,"target":48,"value":1},
      |    {"source":75,"target":41,"value":1},
      |    {"source":75,"target":70,"value":1},
      |    {"source":75,"target":71,"value":1},
      |    {"source":76,"target":64,"value":1},
      |    {"source":76,"target":65,"value":1},
      |    {"source":76,"target":66,"value":1},
      |    {"source":76,"target":63,"value":1},
      |    {"source":76,"target":62,"value":1},
      |    {"source":76,"target":48,"value":1},
      |    {"source":76,"target":58,"value":1}
      |  ]
      |}
    """.stripMargin
}