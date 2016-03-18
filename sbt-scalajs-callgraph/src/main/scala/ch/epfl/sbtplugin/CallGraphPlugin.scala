package ch.epfl.sbtplugin

import org.scalajs.core.ir.Infos
import org.scalajs.core.ir.Infos.ClassInfo
import org.scalajs.core.tools.linker.analyzer.{Analysis, Analyzer, SymbolRequirement}
import org.scalajs.core.tools.sem.Semantics
import sbt._

import scala.language.implicitConversions

object CallGraphPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    lazy val callgraph = TaskKey[Unit]("callgraph", "Export call graph to json")
    lazy val infoTask = TaskKey[Seq[Infos.ClassInfo]]("info-task")
    lazy val analysisTask = TaskKey[scala.collection.Map[String, Analysis.ClassInfo]]("analysis-task")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = {
    Seq(
      callgraph := println("ok"),
      infoTask := {
        println("Not ok")
        Seq[ClassInfo]()
         // (scalaJSIR in Compile).value.data map (_.info)
      },
      analysisTask := {
        val s = infoTask.value
        Analyzer.computeReachability(
          Semantics.Defaults,
          SymbolRequirement.factory("test").none(),
          s, false).classInfos
      }
    )
  }


}
