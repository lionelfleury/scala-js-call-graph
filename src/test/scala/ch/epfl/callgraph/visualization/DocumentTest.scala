package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.CallGraph
import ch.epfl.callgraph.visualization.view.{D3GraphView, HtmlView}
import ch.epfl.callgraph.visualization.controller.D3GraphController
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
  }

  def singleNodeGraph = {
    """{"classes":[{"e":"LFirstClassNode$","i":true,"ne":false,"re":true,"s":[],"in":[],"m":[]}], "methods":[]}"""
  }

  private def resetView(callgraph: CallGraph) = {
    D3GraphController.init(callgraph)
    HtmlView.showLeftNav
    HtmlView.showLayers
    HtmlView.searchList
  }

  @Test def testInitialDOM(): Unit = {
    assertEquals(1, $("#header").length)
    assertEquals(1, $("#nav").length)
    assertEquals(1, $("#main").length)
  }

  @Test def testInitialLayer : Unit = {
    resetView(upickle.read[CallGraph](singleNodeGraph))
    assertEquals(1, $("select option:selected").length)
  }

  @Test def svgDisplaySingleNode : Unit = {
    resetView(upickle.read[CallGraph](singleNodeGraph))
    assertEquals(1, $("svg").find("circle").length)
  }

  @Test def contextMenuHiddenByDefault : Unit = {
    assertFalse($(".context-menu").is(":visible").asInstanceOf[Boolean])
  }

  @Test def clickOnNodeOpenContextMenu : Unit = {
    resetView(upickle.read[CallGraph](singleNodeGraph))
    $("svg").find(".node").each({(li: Html) => {
      $(li).contextmenu()
    }}: scalajs.js.ThisFunction)
    assertTrue($(".context-menu").is(":visible").asInstanceOf[Boolean])
  }

}