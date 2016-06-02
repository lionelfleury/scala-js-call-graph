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
       |<table width="100%">
       |<tr><td id="header" colspan=2><h1>Scala.js Call Graph Visualization</h1></td></tr>
       |<tr valign=top><td id="nav"></td><td id="main"></td></tr>
       |</table>
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
