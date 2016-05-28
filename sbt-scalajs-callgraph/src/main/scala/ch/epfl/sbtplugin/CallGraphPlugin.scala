package ch.epfl.sbtplugin

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
    val callgraph = TaskKey[Unit]("callgraph", "Export callgraph")
    val callgraphDev = TaskKey[Unit]("callgraph-dev", "Export callgraph in development mode")
  }

  import ScalaJSPlugin.autoImport._
  import autoImport._

  private def setting(key: TaskKey[Unit], isDev: Boolean): Setting[_] = key := {
    val log = streams.value.log
    val ir = scalaJSIR.value.data
    val linker = scalaJSLinker.value
    val outputMode = scalaJSOutputMode.value
    val withSourceMap = emitSourceMaps.value
    val semantics = linker.semantics
    val symbolRequirements =
      new BasicLinkerBackend(semantics, outputMode, withSourceMap, LinkerBackend.Config())
        .symbolRequirements

    val infos = Try {
      linker.linkUnit(ir, symbolRequirements, log)
    } match {
      case Success(linkUnit) =>
        linkUnit.infos.values.toSeq
      case Failure(e) =>
        log.warn(e.getMessage)
        log.warn("Non linking program, falling back to all the *.sjsir files on the classpath...")
        ir map (_.info)
    }

    val analysis = Analyzer.computeReachability(semantics, symbolRequirements, infos, false)
    val graph = Graph.createFrom(analysis)
    val file = (artifactPath in key).value
    HTMLFile.writeToFile(file, graph, isDev)
    log.info(s"HTML file created in $file")
  }

  lazy val configSettings: Seq[Setting[_]] = Seq(
    setting(callgraph, isDev = false),
    setting(callgraphDev, isDev = true),
    artifactPath in(Compile, callgraph) := crossTarget.value / "callgraph.html",
    artifactPath in(Test, callgraph) := crossTarget.value / "callgraph-test.html",
    artifactPath in callgraphDev := crossTarget.value / "dev-callgraph.html"
  )

  override lazy val projectSettings: Seq[Setting[_]] = {
    inConfig(Compile)(configSettings) ++
      inConfig(Test)(configSettings)
  }
}
