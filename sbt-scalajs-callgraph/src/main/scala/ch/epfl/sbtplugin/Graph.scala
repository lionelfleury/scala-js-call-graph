package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir._
import Types._
import Infos._
import Definitions._

import scala.collection.mutable
import scala.util.Try

object Graph {

  def displayName(encodedName: String): String = {
      def typeDisplayName(tpe: ReferenceType): String = tpe match {
        case ClassType(encodedName)      => decodeClassName(encodedName)
        case ArrayType(base, dimensions) => "[" * dimensions + decodeClassName(base)
      }

      val (simpleName, paramTypes, resultType) =
        Definitions.decodeMethodName(encodedName)

      simpleName + "(" + paramTypes.map(typeDisplayName).mkString(",") + ")" +
        resultType.fold("")(typeDisplayName)
  }

  // TODO : replace those with the new ir.Infos classes
  private[this] def toClassNode(classInfo: ClassInfo): ClassNode = {
    new ClassNode(
      decodeClassName(classInfo.encodedName), // TODO : look at decodeMethodName too... but buggy!
      classInfo.isExported,
      classInfo.superClass,
      classInfo.interfaces,
      classInfo.methods.map(_.encodedName))
  }

  private[this] def toMethodNode(methodInfo: MethodInfo): MethodNode = {
    new MethodNode(
      methodInfo.encodedName,
      methodInfo.isExported,
      methodInfo.methodsCalled,
      methodInfo.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): Seq[Node] = {
    val graph = mutable.Map[String, Node]()

    def addToGraph(node: Node): Node = graph.getOrElseUpdate(node.encodedName, node)

    classInfos foreach { classInfo: ClassInfo =>
      if (classInfo.isExported) {
        val classNode = toClassNode(classInfo)
        addToGraph(classNode)
      }

      val methodInfos = classInfo.methods

      methodInfos foreach { methodInfo: MethodInfo =>
        if (methodInfo.isExported) {
          val methodNode = toMethodNode(methodInfo)
          addToGraph(methodNode)
        }
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
