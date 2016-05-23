package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.CallGraph
import ch.epfl.callgraph.visualization.view.{D3GraphView, HtmlView}
import org.junit.Assert._
import org.junit.{Before, Test}
import org.scalajs.dom.html._
import org.scalajs.{dom => sdom}
import upickle.{default => upickle}

import scala.scalajs
import scala.scalajs.js
import scala.scalajs.js.Dynamic.global

class DocumentTest {

  val $ = global.jQuery

  @Before
  def setupDocument() : Unit = {
    $("body").html("")
    $("body")
      .append(
        global.jQuery("<div id=\"header\"><h1>Scala.js Call Graph Visualization</h1></div>" +
          "<div id=\"nav\" style=\"overflow:auto\"></div>" +
          "<div id=\"main\" style=\"overflow:auto\"></div>"))
    HtmlView.main() // setup the file upload button
    D3GraphView.setCallGraph(upickle.read[CallGraph](generateGraph))
    HtmlView.updateHtmlAfterLoad(sdom.document.getElementById("nav").asInstanceOf[Div])
    D3GraphView.renderGraph()
  }

  def generateGraph = {
    """{"classes":[{"encodedName":"s_Predef$Triple$2","displayName":"scala.Predef$Triple$2","isExported":true,"superClass":[],"interfaces":[],"methods":[]}]}"""
  }

  @Test def testInitialDOM(): Unit = {
    assertEquals(1, $("#header").length)
    assertEquals(1, $("#nav").length)
    assertEquals(1, $("#main").length)
  }

  @Test def testInitialLayer : Unit = {
    HtmlView.showLayers
    assertEquals(1, $("li > a.active").length)
  }

  @Test def testSvgSingleNode : Unit = {
    println($("svg").html())
    $("svg").find("circle").each({(li: Html) => {
      println($(li).attr("class"))
    //  $(li).contextmenu()
    }}: scalajs.js.ThisFunction)
    assertEquals(1, $("svg").find("circle").length)

  }
/*
  @Test def testSelectedNode : Unit = {
    $("svg").find("circle").each({(li: Html) => {
      println($(li).attr("class"))
      // $(li).contextmenu()
    }}: scalajs.js.ThisFunction) // ThisFunction in mandatory
    //println(Visualization.d3Graph.selectedNode.name)
  }*/

}