package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Infos._

import scala.collection.mutable

object Graph {

  private def toClassNode(ci: ClassInfo, methods: Set[MethodNode]): ClassNode = {
    new ClassNode(ci.encodedName, ci.isExported, ci.superClass, ci.interfaces, methods)
  }

  private def toMethodNode(ci: ClassInfo, mi: MethodInfo, calledFrom: Map[String, List[String]]): MethodNode = {
    val methods = mi.methodsCalled ++ mi.methodsCalledStatically ++ mi.staticMethodsCalled
    new MethodNode(mi.encodedName, mi.isExported, ci.encodedName, methods, calledFrom, mi.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): CallGraph = {
    val classes = mutable.Set[ClassNode]()

    val called = mutable.Map[String, mutable.Map[String, mutable.Set[String]]]()

    for {
      ci <- classInfos
      mi <- ci.methods
      (className, methodNames) <- mi.methodsCalled ++ mi.methodsCalledStatically ++ mi.staticMethodsCalled
      methodName <- methodNames
    } {
      val calledMap = called.getOrElseUpdate(className + methodName, mutable.Map[String, mutable.Set[String]]())
      val calledList = calledMap.getOrElseUpdate(ci.encodedName, mutable.Set[String]())
      calledList.add(mi.encodedName)
    }

    for (ci <- classInfos) {
      val methods = ci.methods.foldLeft(Set[MethodNode]()) { case (acc, mi) =>
        val cf = called.getOrElse(ci.encodedName + mi.encodedName, Map.empty)
        acc + toMethodNode(ci, mi, cf.mapValues(_.toList).toMap)
      }
      classes += toClassNode(ci, methods)
    }
    called.clear()
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
