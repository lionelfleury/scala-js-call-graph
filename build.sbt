import org.scalajs.core.ir.Infos.ClassInfo
import org.scalajs.core.tools.sem.Semantics
import org.scalajs.core.tools.linker.analyzer._

enablePlugins(ScalaJSPlugin)

name := "Scala.js Call Graph"

scalaVersion := "2.11.7"

licenses += ("MIT", url("http://opensource.org/licenses/mit-license.php"))

// Produce a sequence of ClassInfo
lazy val infoTask = TaskKey[Seq[ClassInfo]]("info-task")
infoTask := (scalaJSIR in Compile).value.data map (_.info)

// Creates the analysis and get the Map
lazy val analysisTask = TaskKey[scala.collection.Map[String, Analysis.ClassInfo]]("analysis-task")
analysisTask := {
  val sq = infoTask.value
  Analyzer.computeReachability(
    Semantics.Defaults, 
    SymbolRequirement.factory("test").none(), 
    sq, false).classInfos
}

// Prints the Seq[ClassInfo]
lazy val printTask = TaskKey[Unit]("print-task")
printTask := println(analysisTask.value)