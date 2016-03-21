package ch.epfl.sbtplugin

import org.scalajs.core.ir.Infos
import org.scalajs.core.ir.Infos.ClassInfo
import org.scalajs.core.tools.linker.analyzer.{Analysis, Analyzer, SymbolRequirement}
import org.scalajs.core.tools.sem.Semantics
import ch.epfl.callgraph.utils.Utils._
import org.scalajs.sbtplugin.ScalaJSPlugin
import ScalaJSPlugin.autoImport._
import sbt._

import scala.language.implicitConversions

object CallGraphPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ScalaJSPlugin

  object autoImport {
    lazy val callgraph = TaskKey[Unit]("callgraph", "Export call graph to json")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = {
    Seq(
      callgraph := {
        val infos = (scalaJSIR in Compile).value.data map (_.info)
        val mapInfos = Analyzer.computeReachability(
          Semantics.Defaults,
          SymbolRequirement.factory("test").none(),
          infos, false).classInfos.toMap
        new Graph(mapInfos)
      }
    )
  }


}
