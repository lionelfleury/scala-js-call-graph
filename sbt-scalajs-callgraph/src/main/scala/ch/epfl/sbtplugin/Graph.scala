package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Infos._

import scala.collection.mutable

object Graph {

  private def toClassNode(ci: ClassInfo, methods: Set[MethodNode]): ClassNode = {
    new ClassNode(ci.encodedName, ci.isExported, ci.superClass, ci.interfaces, methods)
  }

  private def toMethodNode(ci: ClassInfo, mi: MethodInfo): MethodNode = {
    val methods = mi.methodsCalled ++ mi.methodsCalledStatically ++ mi.staticMethodsCalled
    new MethodNode(mi.encodedName, mi.isExported, ci.encodedName, methods, mi.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): CallGraph = {
    val classes = mutable.Set[ClassNode]()

    for (classInfo <- classInfos) {
      val methods = classInfo.methods.map(toMethodNode(classInfo, _))
      classes += toClassNode(classInfo, methods.toSet)
    }

    CallGraph(classes.toSet)
  }

  def writeToFile(graph: CallGraph, file: File): Unit = {
    val json = upickle.default.write(graph)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(json)
    bw.flush()
    bw.close()
  }
}
