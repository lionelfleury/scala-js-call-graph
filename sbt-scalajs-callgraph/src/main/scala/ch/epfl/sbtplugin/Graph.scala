package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions._
import org.scalajs.core.ir.Infos._
import org.scalajs.core.ir.Types._
import org.scalajs.core.ir._

import scala.collection.mutable

object Graph {

  def decodeMethodName(encodedName: String): (String, List[ReferenceType], Option[ReferenceType]) = {
    val (simpleName, privateAndSigString) =
      if (isConstructorName(encodedName)) {
        val privateAndSigString =
          if (encodedName == "init___") ""
          else encodedName.stripPrefix("init___") + "__"
        ("<init>", privateAndSigString)
      } else {
        val pos = encodedName.indexOf("__")
        if (pos != -1) {
          val pos2 =
            if (!encodedName.substring(pos + 2).startsWith("p")) pos
            else encodedName.indexOf("__", pos + 2)
          (encodedName.substring(0, pos), encodedName.substring(pos2 + 2))
        } else {
          (encodedName, "")
        }
      }

    // -1 preserves trailing empty strings
    val parts = privateAndSigString.split("__", -1).toSeq
    val paramsAndResultStrings =
      if (parts.headOption.exists(_.startsWith("p"))) parts.tail
      else parts

    val paramStrings :+ resultString = paramsAndResultStrings

    val paramTypes = paramStrings.map(decodeReferenceType).toList
    val resultType =
      if (resultString == "") None // constructor or reflective proxy
      else Some(decodeReferenceType(resultString))

    (simpleName, paramTypes, resultType)
  }

  def displayName(encodedName: String): String = {
    def typeDisplayName(tpe: ReferenceType): String = tpe match {
      case ClassType(encodedName) => decodeClassName(encodedName)
      case ArrayType(base, dimensions) => "[" * dimensions + decodeClassName(base)
    }

    val (simpleName, paramTypes, resultType) =
      decodeMethodName(encodedName)

    simpleName + "(" + paramTypes.map(typeDisplayName).mkString(",") + ")" +
      resultType.fold("")(typeDisplayName)
  }

  // TODO : replace those with the new ir.Infos classes
  private[this] def toClassNode(classInfo: ClassInfo): ClassNode = {
    val encodedName = classInfo.encodedName
    val methods = classInfo.methods.map(_.encodedName).toSet
    new ClassNode(
      encodedName,
      decodeClassName(encodedName),
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
      displayName(encodedName),
      methodInfo.isExported,
      methodsCalled,
      methodInfo.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): Seq[Node] = {
    val graph = mutable.Map[String, Node]()

    def addToGraph(node: Node): Node =
      graph.getOrElseUpdate(node.encodedName, node)

    for (classInfo <- classInfos) {
//      TODO : CHECK IF CLASS REALLY MATTTERS !!
      val classNode = toClassNode(classInfo)
      addToGraph(classNode)

      for (methodInfo <- classInfo.methods) {
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
