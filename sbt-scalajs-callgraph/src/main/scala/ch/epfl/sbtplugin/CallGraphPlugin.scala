package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker.backend._
import org.scalajs.sbtplugin.Implicits._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPluginInternal.scalaJSLinker
import sbt.Keys._
import sbt._

object CallGraphPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = ScalaJSPlugin

  object autoImport {
    val callgraph = TaskKey[Unit]("callgraph", "Export callgraph to json")
  }

  import ScalaJSPlugin.autoImport._
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = {
    Seq(
      callgraph := {
        val log = streams.value.log
        val ir = (scalaJSIR in Compile).value.data
        val linker = (scalaJSLinker in Compile).value
        val outputMode = (scalaJSOutputMode in Compile).value
        val withSourceMap = (emitSourceMaps in Compile).value
        val backendConfig = LinkerBackend.Config()
        val symbolRequirements =
          new BasicLinkerBackend(linker.semantics, outputMode, withSourceMap, backendConfig).symbolRequirements

        val linkUnit = linker.linkUnit(ir, symbolRequirements, log)
        val mapInfos = linkUnit.infos

        val graph = Graph.createFrom(mapInfos.values.toSeq)

        val jsonFile = crossTarget.value / "graph.json"
        Graph.writeToFile(graph, jsonFile)
        log.info(s"CallGraph file created in $jsonFile")

        val htmlFile = crossTarget.value / "index.html"
        HTMLFile.writeToFile(htmlFile)
        log.info(s"HTML file created in $htmlFile")
      }
    )
  }
}
