package ch.epfl.sbtplugin

import org.scalajs.core.tools.linker._
import org.scalajs.core.tools.linker.backend._
import org.scalajs.core.tools.linker.frontend._
import org.scalajs.jsenv._
import org.scalajs.sbtplugin.Implicits._
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

import scala.util.{Failure, Success}

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
        val linker = {
          val opts = (scalaJSOptimizerOptions in Compile).value
          val semantics = (scalaJSSemantics in Compile).value
          val outputMode = (scalaJSOutputMode in Compile).value
          val withSourceMap = (emitSourceMaps in Compile).value

          val frontendConfig = LinkerFrontend.Config()
            .withCheckIR(opts.checkScalaJSIR)

          val backendConfig = LinkerBackend.Config()
            .withCustomOutputWrapper(scalaJSOutputWrapper.value)
            .withPrettyPrint(opts.prettyPrintFullOptJS)

          val newLinker = { () =>
            Linker(semantics, outputMode, withSourceMap, opts.disableOptimizer,
              opts.parallel, opts.useClosureCompiler, frontendConfig,
              backendConfig)
          }

          new ClearableLinker(newLinker, opts.batchMode)
        }
        // TODO : check for correct environment
        val env = (resolvedJSEnv in Compile).value.asInstanceOf[LinkingUnitJSEnv]
        val unit = linker.linkUnit(ir, env.symbolRequirements, log)
        val mapInfos = unit.infos

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
