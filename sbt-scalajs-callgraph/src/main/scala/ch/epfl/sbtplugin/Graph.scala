package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions._
import org.scalajs.core.ir.Infos._
import org.scalajs.core.ir.Types._
import org.scalajs.core.ir._

import scala.collection.mutable

object Graph {

  private def decodeMethodName(encodedName: String): (String, List[ReferenceType], Option[ReferenceType]) = {
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

  private def displayName(encodedName: String): String = {
    def typeDisplayName(tpe: ReferenceType): String = tpe match {
      case ClassType(encodedName) => decodeClassName(encodedName)
      case ArrayType(base, dimensions) => "[" * dimensions + decodeClassName(base)
    }

    val (simpleName, paramTypes, resultType) = decodeMethodName(encodedName)

    simpleName + "(" + paramTypes.map(typeDisplayName).mkString(",") + ")" +
      resultType.fold("")(typeDisplayName)
  }

  private[this] def toClassNode(ci: ClassInfo): ClassNode = {
    val encodedName = ci.encodedName
    val methods = ci.methods.map(_.encodedName).toSet
    new ClassNode(
      encodedName,
      decodeClassName(encodedName),
      ci.isExported,
      ci.superClass,
      ci.interfaces,
      methods)
  }

  private[this] def toMethodNode(mi: MethodInfo): MethodNode = {
    val encodedName = mi.encodedName
    val methodsCalled = mutable.Set[String]()
    methodsCalled ++= mi.methodsCalled.values.flatten
    methodsCalled ++= mi.methodsCalledStatically.values.flatten
    methodsCalled ++= mi.staticMethodsCalled.values.flatten
    new MethodNode(
      encodedName,
      displayName(encodedName),
      mi.isExported,
      methodsCalled.toSet,
      mi.instantiatedClasses)
  }

  def createFrom(classInfos: Seq[ClassInfo]): CallGraph = {
    val classes = mutable.Set[ClassNode]()
    val methods = mutable.Set[MethodNode]()

    for (classInfo <- classInfos) {
      classes += toClassNode(classInfo)
      for (methodInfo <- classInfo.methods) {
        methods += toMethodNode(methodInfo)
      }
    }

    CallGraph(classes.toSet, methods.toSet)
  }

  def writeToFile(graph: CallGraph, file: File): Unit = {
    val json = upickle.default.write[CallGraph](graph)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(json)
    bw.flush()
    bw.close()
  }
}
