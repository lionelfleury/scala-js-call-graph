package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions._
import org.scalajs.core.ir.Infos._
import org.scalajs.core.ir.Types._
import org.scalajs.core.ir._

import scala.collection.mutable

object Graph {

  def displayName(encodedName: String): String = {
    def typeDisplayName(tpe: ReferenceType): String = tpe match {
      case ClassType(encodedName) => decodeClassName(encodedName)
      case ArrayType(base, dimensions) => "[" * dimensions + decodeClassName(base)
    }

    val (simpleName, paramTypes, resultType) =
      Definitions.decodeMethodName(encodedName)

    simpleName + "(" + paramTypes.map(typeDisplayName).mkString(",") + ")" +
      resultType.fold("")(typeDisplayName)
  }

  // TODO : replace those with the new ir.Infos classes
  private[this] def toClassNode(classInfo: ClassInfo): ClassNode = {
    val encodedName = classInfo.encodedName
    val methods = classInfo.methods.map(_.encodedName).toSet
    new ClassNode(
      encodedName,
      decodeClassName(encodedName), // TODO : look at decodeMethodName too... but buggy!
      classInfo.isExported,
      classInfo.superClass,
      classInfo.interfaces,
      methods)
  }

  private[this] def toMethodNode(methodInfo: MethodInfo): MethodNode = {
    val encodedName = methodInfo.encodedName
    val methodsCalled =
      (methodInfo.methodsCalled.values ++ methodInfo.methodsCalledStatically.values).flatten.toSet
    new MethodNode(
      encodedName,
      encodedName,
      methodInfo.isExported,
      methodsCalled,
      methodInfo.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): Seq[Node] = {
    val graph = mutable.Map[String, Node]()

    def addToGraph(node: Node): Node =
      graph.getOrElseUpdate(node.encodedName, node)

    classInfos foreach { classInfo: ClassInfo =>
      val classNode = toClassNode(classInfo)
      addToGraph(classNode)

      val methodInfos = classInfo.methods

      methodInfos foreach { methodInfo: MethodInfo =>
        val methodNode = toMethodNode(methodInfo)
        addToGraph(methodNode)
      }
    }
    graph.values.toSeq
  }

  def writeToFile(graph: Seq[Node], file: File): Unit = {
    val json = upickle.default.write(graph)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(json)
    bw.flush()
    bw.close()
  }
}
