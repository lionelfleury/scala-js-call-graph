package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.tools.linker.analyzer.Analysis._

import scala.collection.mutable

object Graph {

  private[this] def toClassNode(classInfo: ClassInfo): ClassNode = {
    val methodInfos =
      (classInfo.methodInfos ++ classInfo.staticMethodInfos) mapValues (_.encodedName)
    new ClassNode(
      classInfo.encodedName,
      classInfo.displayName,
      classInfo.nonExistent,
      methodInfos.values.toSet)
  }

  private[this] def toMethodNode(methodInfo: MethodInfo): MethodNode = {
    new MethodNode(
      methodInfo.encodedName,
      methodInfo.displayName,
      methodInfo.nonExistent,
      methodInfo.owner.encodedName,
      methodInfo.isStatic)
  }

  def createFrom(classInfos: Seq[ClassInfo]): Seq[Node] = {
    val graph = mutable.Map[String, Node]()

    def addToGraph(node: Node): Node = graph.getOrElseUpdate(node.encodedName, node)

    classInfos foreach { classInfo: ClassInfo =>
      val classNode = toClassNode(classInfo)
      addToGraph(classNode)

      val methodInfos =
        (classInfo.methodInfos ++ classInfo.staticMethodInfos).values

      methodInfos foreach { methodInfo: MethodInfo =>
        val methodNode = toMethodNode(methodInfo)
        addToGraph(methodNode)
      }

      /* Inverse the existing edges */
      classInfo.instantiatedFrom foreach {
        case from: FromMethod => {
          val method = from.methodInfo
          val methodNode = addToGraph(toMethodNode(method))
          /* Add a link from the method to the class */
          methodNode.out += classInfo.encodedName
        }
        case _ => /* Not implemented yet */
      }
    }
    graph.values.toSeq
  }

  def writeToFile(graph: Seq[Node], file: File): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    val json = upickle.default.write(graph)
    bw.write(json)
    bw.flush()
    bw.close()
  }
}
