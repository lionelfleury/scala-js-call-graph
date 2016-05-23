package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.tools.linker.analyzer.Analysis
import org.scalajs.core.tools.linker.analyzer.Analysis._

import scala.collection.mutable

object Graph {

  type Calls = Map[String, Seq[String]]
  type CallsMap = Map[String, Calls]

  private def toMethodNode(ci: ClassInfo, mi: MethodInfo, callsMap: CallsMap): MethodNode = {
    val calls = callsMap.getOrElse(mi.owner.encodedName + mi.encodedName, Map.empty)
    val calledFrom = fromToList(mi.calledFrom)
    new MethodNode(mi.encodedName, mi.isExported, mi.nonExistent, mi.isReachable, ci.encodedName, calls, calledFrom)
  }

  private def toClassNode(ci: ClassInfo, methods: Seq[MethodNode]): ClassNode = {
    val superClass = if (ci.superClass != null) Some(ci.superClass.encodedName) else None
    val interfaces = ci.ancestors.map(_.encodedName)
    new ClassNode(ci.encodedName, ci.isExported, ci.nonExistent, ci.isNeededAtAll, superClass, interfaces, methods)
  }

  /**
    * Convert a sequence of Analysis.From to Map from class to method
    *
    * @param froms the list of From
    * @return a mapping from classes to methods
    */
  private def fromToList(froms: Seq[From]): Calls = {
    val calls = mutable.Map[String, Seq[String]]()
    def addToCalls(key: String, value: String): Unit = {
      val seq = calls.getOrElseUpdate(key, Seq.empty)
      calls.update(key, value +: seq)
    }
    froms foreach {
      case FromMethod(method) => addToCalls(method.owner.encodedName, method.encodedName)
      case FromCore(name) => addToCalls("core", name)
      case FromExports => addToCalls("exports", "")
    }
    calls.toMap
  }

  /**
    * Create a reverse graph using calledFrom
    *
    * @param classInfos the class infos
    * @return a map of map, associating a method to classes to methods
    */
  private def reverseEdges(classInfos: Iterable[ClassInfo]): CallsMap = {
    val map = mutable.Map[String, mutable.Map[String, mutable.Set[String]]]()
    for {
      classInfo <- classInfos
      methodInfo <- classInfo.methodInfos.values ++ classInfo.staticMethodInfos.values
      FromMethod(method) <- methodInfo.calledFrom
      key = method.owner.encodedName + method.encodedName
      bucket = map.getOrElseUpdate(key, mutable.Map[String, mutable.Set[String]]())
      called = bucket.getOrElseUpdate(methodInfo.owner.encodedName, mutable.Set[String]())
    } called += methodInfo.encodedName
    map.mapValues(_.mapValues(_.toSeq).toMap).toMap
  }

  /**
    * Create the call graph from the given analysis
    *
    * @param analysis the analysis
    * @return the call graph
    */
  def createFrom(analysis: Analysis): CallGraph = {
    val classInfos = analysis.classInfos.values
    val callsMap = reverseEdges(classInfos)
    val classes = mutable.Set[ClassNode]()

    for (classInfo <- classInfos) {
      val methodInfos = classInfo.methodInfos.values ++ classInfo.staticMethodInfos.values
      val methods = methodInfos.foldLeft(Set[MethodNode]()) {
        case (set, methodInfo) => set + toMethodNode(classInfo, methodInfo, callsMap)
      }
      classes += toClassNode(classInfo, methods.toSeq)
    }
    CallGraph(classes.toSeq)
  }

  /**
    * Serialize and write to a file
    *
    * @param graph the call graph
    * @param file  the file to write in
    */
  def writeToFile(graph: CallGraph, file: File): Unit = {
    val json = upickle.default.write(graph)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(json)
    bw.flush()
    bw.close()
  }
}
