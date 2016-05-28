package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

object HTMLFile {

  private val pathDev = "../../../target/scala-2.11/scalajs-callgraph-"
  private val pathRelease = "https://rawgit.com/lionelfleury/scala-js-call-graph/release/scalajs-callgraph-"

  private def content(graph: String, isDev: Boolean): String = {
    val pathCSS = if (isDev) "../../../scalajs-callgraph-" else pathRelease
    val pathToJS = if (isDev) pathDev else pathRelease
    val opt = if (isDev) "fastopt.js" else "opt.js"
    val jsOpt = pathToJS + opt
    val jsDeps = pathToJS + "jsdeps.js"
    val jsLauncher = pathToJS + "launcher.js"
    s"""
       |<!DOCTYPE html>
       |<html>
       |<meta charset="UTF-8">
       |<title>Scala.js Call Graph Visualization</title>
       |<link rel="stylesheet" type="text/css" href="${pathCSS}style.css">
       |<body>
       |<div id="header"><h1>Scala.js Call Graph Visualization</h1></div>
       |<div id="nav" style="overflow:auto"></div>
       |<div id="main" style="overflow:auto"></div>
       |<div id="callgraph" style="display: none;">$graph</div>
       |<script type="text/javascript" src="$jsOpt"></script>
       |<script type="text/javascript" src="$jsDeps"></script>
       |<script type="text/javascript" src="$jsLauncher"></script>
       |</body>
       |</html>
    """.stripMargin
  }

  def writeToFile(file: File, graph: String, isDev: Boolean): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content(graph, isDev))
    bw.flush()
    bw.close()
  }

}
