package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker.analyzer.Analyzer
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend, OutputMode}
import org.scalajs.sbtplugin.ScalaJSPlugin
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
        val config = LinkerBackend.Config()
        val linkerBackEnd =
          new BasicLinkerBackend(semantics, OutputMode.Default, true, config)
        val symbolRequirement = linkerBackEnd.symbolRequirements
        val allowAddingSyntheticMethods = false
        val infos = (scalaJSIR in Compile).value.data map (_.info)
        val mapInfos = Analyzer.computeReachability(
          semantics,
          symbolRequirement,
          infos,
          allowAddingSyntheticMethods
        ).classInfos.toMap

        new Graph(mapInfos)
      }
    )
  }
}
