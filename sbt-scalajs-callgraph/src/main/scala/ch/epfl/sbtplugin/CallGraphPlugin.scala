package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker.analyzer.Analyzer
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend, OutputMode}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

object CallGraphPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = ScalaJSPlugin

  object autoImport {
    lazy val callgraph = TaskKey[Unit]("callgraph", "Export callgraph to json")
  }

  import ScalaJSPlugin.autoImport._
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = {
    Seq(
      callgraph := {
        val semantics = scalaJSSemantics.value
        val withSourceMap = true
        val config = LinkerBackend.Config()
        val linkerBackEnd =
          new BasicLinkerBackend(semantics, OutputMode.Default, withSourceMap, config)
        val symbolRequirements = linkerBackEnd.symbolRequirements
        val allowAddingSyntheticMethods = false
        val classInfos = (scalaJSIR in Compile).value.data map (_.info)
        val mapInfos = Analyzer.computeReachability(
          semantics,
          symbolRequirements,
          classInfos,
          allowAddingSyntheticMethods
        ).classInfos

        val graph = Graph.createFrom(mapInfos.values.toSeq)
        Graph.writeToFile(graph, crossTarget.value / "graph.json")
      }
    )
  }
}
