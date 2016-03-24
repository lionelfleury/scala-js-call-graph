package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker.analyzer.{Analyzer, SymbolRequirement}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

import scala.language.implicitConversions

object CallGraphPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    lazy val callgraph = TaskKey[Unit]("callgraph", "Export call graph to json")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = {
    Seq(
      callgraph := {
        val infos = (scalaJSIR in Compile).value.data map (_.info)
        val mapInfos = Analyzer.computeReachability(
          scalaJSSemantics.value,
          SymbolRequirement.factory("test").none(),
          infos, false).classInfos.toMap
        new Graph(mapInfos)
      }
    )
  }
}
