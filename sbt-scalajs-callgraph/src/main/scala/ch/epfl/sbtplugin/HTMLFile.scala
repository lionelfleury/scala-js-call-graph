package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

object HTMLFile {

  val content =
    """
      |<!DOCTYPE html>
      |<html>
      |<meta charset="UTF-8">
      |<title>Scala.js Call Graph Visualization</title>
      |<link rel="stylesheet" type="text/css" href="https://rawgit.com/lionelfleury/scala-js-call-graph/release/style.css">
      |<body>
      |<div id="header"><h1>Scala.js Call Graph Visualization</h1></div>
      |<div id="nav" style="overflow:auto"></div>
      |<div id="main" style="overflow:auto"></div>
      |<script type="text/javascript" src="https://rawgit.com/lionelfleury/scala-js-call-graph/release/scalajs-callgraph-opt.js"></script>
      |<script type="text/javascript" src="https://rawgit.com/lionelfleury/scala-js-call-graph/release/scalajs-callgraph-jsdeps.min.js"></script>
      |<script type="text/javascript" src="https://rawgit.com/lionelfleury/scala-js-call-graph/release/scalajs-callgraph-launcher.js"></script>
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
