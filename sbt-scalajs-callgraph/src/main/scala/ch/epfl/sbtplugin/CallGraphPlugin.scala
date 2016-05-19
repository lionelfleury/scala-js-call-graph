package ch.epfl.sbtplugin

import org.scalajs.core.ir.Infos
import org.scalajs.core.tools.linker.analyzer.Analyzer
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend}
import org.scalajs.sbtplugin.Implicits._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPluginInternal.scalaJSLinker
import sbt.Keys._
import sbt._

import scala.util.{Failure, Success, Try}

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
        val semantics = linker.semantics
        val symbolRequirements =
          new BasicLinkerBackend(semantics, outputMode, withSourceMap, LinkerBackend.Config())
            .symbolRequirements

        Try {
          linker.linkUnit(ir, symbolRequirements, log)
        } match {
          case Success(linkUnit) =>
            val infos = linkUnit.infos.values.toSeq
            createCallGraph(infos)
          case Failure(e) =>
            log.warn(e.getMessage)
            log.warn("Non linking program, falling back to all the *.sjsir files on the classpath...")
            val infos = ir map (_.info)
            createCallGraph(infos)
        }

        def createCallGraph(infos: Seq[Infos.ClassInfo]) = {
          val analysis = Analyzer.computeReachability(semantics, symbolRequirements, infos, false)
          val graph = Graph.createFrom(analysis.classInfos.values.toSeq)

          val jsonFile = crossTarget.value / "graph.json"
          Graph.writeToFile(graph, jsonFile)
          log.info(s"CallGraph file created in $jsonFile")

          val htmlFile = crossTarget.value / "index.html"
          HTMLFile.writeToFile(htmlFile)g
          log.info(s"HTML file created in $htmlFile")
        }

      }
    )
  }
}
