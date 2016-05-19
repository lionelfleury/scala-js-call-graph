package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.tools.linker.analyzer.Analysis._

import scala.collection.mutable

object Graph {

  type CallsMapping = Map[String, Map[String, Seq[String]]]

  private def toMethodNode(ci: ClassInfo, mi: MethodInfo, reverse: CallsMapping): MethodNode = {
    new MethodNode(mi.encodedName, mi.isExported, mi.nonExistent, ci.encodedName, reverse.getOrElse(mi.encodedName, Map[String, Seq[String]]()), fromToList(mi.calledFrom))
  }

  private def toClassNode(ci: ClassInfo, methods: Set[MethodNode]): ClassNode = {
    val parent = if(ci.superClass == null) None else Some(ci.superClass.encodedName)
    new ClassNode(ci.encodedName, ci.isExported, ci.nonExistent, parent, ci.ancestors.map(_.encodedName), methods)
  }

  /**
    * Convert a sequence of Analysis.From to Map from class to method
    * @param from the list of from
    * @return a mapping from classes to methods
    */
  private def fromToList(from: Seq[From]) : Map[String, Seq[String]] = {
    from.map {
      case FromMethod(method) => (method.owner.encodedName, method.encodedName)
      case FromCore(name) => ("core", name)
      case FromExports => ("exports", "")
    }.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
    * Create a reverse graph using calledFrom
    * @param mis methodInfoDequence, the methods that compose our graph
    * @return a map of map, associating a method to classes to methods
    */
  private def reverseEdges(mis: Seq[MethodInfo]) : CallsMapping = {
    val map = mutable.Map[String, mutable.Map[String, mutable.Set[String]]]()
    mis foreach(mi => {
      mi.calledFrom.foreach {
        case FromMethod(f) =>
          val bucket = map.getOrElseUpdate(f.encodedName, mutable.HashMap[String, mutable.Set[String]]())
          val called = bucket.getOrElseUpdate(mi.owner.encodedName, mutable.HashSet[String]())
          called += mi.encodedName
        case _ =>
      }
    })
    map.mapValues(_.mapValues(_.toSeq).toMap).toMap
  }

  def createFrom(classInfos: Seq[ClassInfo]): CallGraph = {
    val methods = classInfos.flatMap(x => (x.methodInfos ++ x.staticMethodInfos).values)
    val reverse = reverseEdges(methods)
    val classes = new mutable.HashSet[ClassNode]()

    for (ci <- classInfos) {
      val methods = (ci.methodInfos ++ ci.staticMethodInfos).values.foldLeft(Set[MethodNode]()) { case (acc, mi) =>
        acc + toMethodNode(ci, mi, reverse)
      }
      classes += toClassNode(ci, methods)
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
