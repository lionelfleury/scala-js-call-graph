package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.StandardCopyOption._
import java.nio.file.{Files, Paths}

import sbt._

class FileGenerator(graph: String, isDev: Boolean) {

  private val path = if (isDev) "../../../visualisation/target/scala-2.11/" else "./res/"
  private val pathStyle = if (isDev) "../../../" else "./res/"
  private val opt = if (isDev) "callgraph-fastopt.js" else "callgraph-opt.js"
  private val deps = "callgraph-jsdeps.js"
  private val launcher = "callgraph-launcher.js"
  private val style = "callgraph-style.css"

  private lazy val content =
    s"""
       |<!DOCTYPE html>
       |<html>
       |<meta charset="UTF-8">
       |<title>Scala.js Call Graph Visualization</title>
       |<link rel="stylesheet" type="text/css" href="$pathStyle$style">
       |<body>
       |<table width="100%">
       |<tr><td id="header" colspan=2><h1>Scala.js Call Graph Visualization</h1></td></tr>
       |<tr valign=top><td id="nav"></td><td id="main"></td></tr>
       |</table>
       |<div id="callgraph" style="display: none;">$graph</div>
       |<script type="text/javascript" src="$path$opt"></script>
       |<script type="text/javascript" src="$path$deps"></script>
       |<script type="text/javascript" src="$path$launcher"></script>
       |</body>
       |</html>
    """.stripMargin

  private def copyFile(name: String, destination: File): Unit = {
    val src = getClass.getResourceAsStream("/" + name)
    val tgt = destination / name
    Files.copy(src, Paths.get(tgt.absolutePath), REPLACE_EXISTING)
    src.close()
  }

  def createFiles(name: String, destination: File): Unit = {
    if (!isDev) {
      val dest = destination / "res"
      Files.createDirectories(Paths.get(dest.absolutePath))
      val files = Seq(opt, deps, launcher, style)
      for (file <- files) copyFile(file, dest)
    }
    val bw = new BufferedWriter(new FileWriter(destination / name))
    bw.write(content)
    bw.flush()
    bw.close()
  }
}
