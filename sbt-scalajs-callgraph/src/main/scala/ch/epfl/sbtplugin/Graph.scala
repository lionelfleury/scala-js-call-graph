package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions._
import org.scalajs.core.ir.Infos._
import org.scalajs.core.ir.Types._

import scala.collection.mutable

object Graph {

  private[this] def toClassNode(ci: ClassInfo, methods: Set[MethodNode]): ClassNode = {
    val encodedName = ci.encodedName
    new ClassNode(
      encodedName,
      ci.isExported,
      ci.superClass,
      ci.interfaces,
      methods)
  }

  private[this] def toMethodNode(mi: MethodInfo, ci: ClassInfo): MethodNode = {
    val encodedName = mi.encodedName
    val methodsCalled =  mi.methodsCalled ++ mi.methodsCalledStatically ++ mi.staticMethodsCalled
    new MethodNode(
      encodedName,
      mi.isExported,
      ci.encodedName,
      methodsCalled,
      mi.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): CallGraph = {
    val classes = mutable.Set[ClassNode]()

    for (classInfo <- classInfos) {
      val classMethods = classInfo.methods.map(toMethodNode(_, classInfo))
      classes += toClassNode(classInfo, classMethods.toSet)
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
