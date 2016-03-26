package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker.analyzer.Analyzer
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend, OutputMode}
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

import scala.util.{Failure, Success}

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

        val log = streams.value.log
        val graph = Graph.createFrom(mapInfos.values.toSeq)
        val file = crossTarget.value / "graph.json"
        Graph.writeToFile(graph, file) match {
          case Success(_) => log.info(s"callgraph created in $file")
          case Failure(e) => sbt.toError(Some(e.getMessage))
        }
      }
    )
  }
}
