package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

/**
  * Created by Lionel on 07/05/16.
  */
object HTMLFile {

  // TODO: Change content to reflect some online .js files for productivity code
  // TODO: Same idea for the style part...
  val content =
    """
      |<!DOCTYPE html>
      |<html>
      |<meta charset="UTF-8">
      |<title>Scala.js Call Graph Visualization</title>
      |<style>
      |#header{background-color:black;color:white;text-align:center;padding:5px;}
      |#nav{background-color:#eeeeee;height:800px;width:300px;float:left;padding:5px;}
      |#main{width:800px;float:left;padding:10px;}
      |.node{cursor:pointer;stroke:#3182bd;stroke-width:1.5px;}
      |.node text{fill:black;stroke-width:0px;font:9pxhelvetica;}
      |.link{fill:none;stroke:#A0A0A0;stroke-width:1.5px;}
      |.context-menu{display:none;position:absolute;z-index:10;padding:12px0;width:240px;background-color:#fff;border:solid1px#dfdfdf;box-shadow:1px1px2px#cfcfcf;}
      |.context-menu--active{display:block;}.context-menu__items{list-style:none;margin:0;padding:0;}
      |.context-menu__item{display:block;margin-bottom:4px;}.context-menu__item:last-child{margin-bottom:0;}
      |.context-menu__link{display:block;padding:4px12px;color:#0066aa;text-decoration:none;}
      |.context-menu__link:hover{color:#fff;background-color:#0066aa;}
      |.layersa.active{text-decoration:none;pointer-events:none;cursor:default;color:black;}
      |</style>
      |<body>
      |<div id="header"><h1>Scala.js Call Graph Visualization</h1></div>
      |<div id="nav" style="overflow:auto"></div>
      |<div id="main" style="overflow:auto"></div>
      |<script type="text/javascript" src="../../../target/scala-2.11/scalajs-callgraph-fastopt.js"></script>
      |<script type="text/javascript" src="../../../target/scala-2.11/scalajs-callgraph-jsdeps.js"></script>
      |<script type="text/javascript" src="../../../target/scala-2.11/scalajs-callgraph-launcher.js"></script>
      |</body>
      |</html>
    """.stripMargin

  def writeToFile(file: File): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.flush()
    bw.close()
  }

}
