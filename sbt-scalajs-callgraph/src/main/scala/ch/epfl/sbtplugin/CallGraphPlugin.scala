package ch.epfl.sbtplugin

import org.scalajs.sbtplugin.ScalaJSPluginInternal.scalaJSLinker
import org.scalajs.core.tools.linker._
import org.scalajs.core.tools.linker.backend._
import org.scalajs.core.tools.linker.frontend._
import org.scalajs.jsenv._
import org.scalajs.sbtplugin.Implicits._
import org.scalajs.sbtplugin.ScalaJSPlugin
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
        val opts = (scalaJSOptimizerOptions in Compile).value
        val semantics = linker.semantics
        val outputMode = (scalaJSOutputMode in Compile).value
        val withSourceMap = (emitSourceMaps in Compile).value
        val backendConfig = LinkerBackend.Config()
          .withCustomOutputWrapper(scalaJSOutputWrapper.value)
          .withPrettyPrint(opts.prettyPrintFullOptJS)
        val symbolRequirements =
          new BasicLinkerBackend(semantics, outputMode, withSourceMap, backendConfig).symbolRequirements

        val linkUnit = linker.linkUnit(ir, symbolRequirements, log)
        val mapInfos = linkUnit.infos

        val graph = Graph.createFrom(mapInfos.values.toSeq)
        val file = crossTarget.value / "graph.json"
        Graph.writeToFile(graph, file)
        log.info(s"callgraph created in $file")
      }
    )
  }
}
