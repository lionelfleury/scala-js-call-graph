package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Infos._

import scala.collection.mutable
import scala.util.Try

object Graph {

  // TODO : replace those with the new ir.Infos classes
  private[this] def toClassNode(classInfo: ClassInfo): ClassNode = {
    new ClassNode(
      classInfo.encodedName,
      classInfo.encodedName,
      classInfo.isExported,
      classInfo.methods.map(_.encodedName).toSet)
  }

  private[this] def toMethodNode(methodInfo: MethodInfo): MethodNode = {
    new MethodNode(
      methodInfo.encodedName,
      methodInfo.encodedName,
      methodInfo.isExported,
      "TODO",
      methodInfo.isStatic)
  }

  def createFrom(classInfos: Seq[ClassInfo]): Seq[Node] = {
    val graph = mutable.Map[String, Node]()

    def addToGraph(node: Node): Node = graph.getOrElseUpdate(node.encodedName, node)

    classInfos foreach { classInfo: ClassInfo =>
      val classNode = toClassNode(classInfo)
      if (classInfo.isExported)
        addToGraph(classNode)

      val methodInfos = classInfo.methods

      methodInfos foreach { methodInfo: MethodInfo =>
        val methodNode = toMethodNode(methodInfo)
        addToGraph(methodNode)
      }
    }
    graph.values.toSeq
  }

  def writeToFile(graph: Seq[Node], file: File): Try[Unit] = {
    val json = upickle.default.write(graph)
    Try {
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(json)
      bw.flush()
      bw.close()
    }
  }
}
