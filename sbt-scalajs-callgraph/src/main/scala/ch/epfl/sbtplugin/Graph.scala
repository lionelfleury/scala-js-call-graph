package ch.epfl.sbtplugin

import java.io.{BufferedWriter, File, FileWriter}

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions._
import org.scalajs.core.ir.Infos._
import org.scalajs.core.ir.Types._

import scala.collection.mutable

object Graph {

  /** Decodes a class name encoded with [[encodeClassName]]. */
  private def decodeClassName(encodedName: String): String = {
    val encoded =
      if (encodedName.charAt(0) == '$') encodedName.substring(1)
      else encodedName
    val base = decompressedClasses.getOrElse(encoded, {
      decompressedPrefixes collectFirst {
        case (prefix, decompressed) if encoded.startsWith(prefix) =>
          decompressed + encoded.substring(prefix.length)
      } getOrElse {
        assert(!encoded.isEmpty && encoded.charAt(0) == 'L',
          s"Cannot decode invalid encoded name '$encodedName'")
        encoded.substring(1)
      }
    })
    base.replace("_", ".").replace("$und", "_")
  }

  private val compressedClasses: Map[String, String] = Map(
    "java_lang_Object" -> "O",
    "java_lang_String" -> "T",
    "scala_Unit" -> "V",
    "scala_Boolean" -> "Z",
    "scala_Char" -> "C",
    "scala_Byte" -> "B",
    "scala_Short" -> "S",
    "scala_Int" -> "I",
    "scala_Long" -> "J",
    "scala_Float" -> "F",
    "scala_Double" -> "D"
  ) ++ (
    for (index <- 2 to 22)
      yield s"scala_Tuple$index" -> ("T"+index)
    ) ++ (
    for (index <- 0 to 22)
      yield s"scala_Function$index" -> ("F"+index)
    )

  private val decompressedClasses: Map[String, String] =
    compressedClasses map { case (a, b) => (b, a) }

  private val compressedPrefixes = Seq(
    "scala_scalajs_runtime_" -> "sjsr_",
    "scala_scalajs_" -> "sjs_",
    "scala_collection_immutable_" -> "sci_",
    "scala_collection_mutable_" -> "scm_",
    "scala_collection_generic_" -> "scg_",
    "scala_collection_" -> "sc_",
    "scala_runtime_" -> "sr_",
    "scala_" -> "s_",
    "java_lang_" -> "jl_",
    "java_util_" -> "ju_"
  )

  private val decompressedPrefixes: Seq[(String, String)] =
    compressedPrefixes map { case (a, b) => (b, a) }

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

  private[this] def toClassNode(ci: ClassInfo, methods: Set[MethodNode]): ClassNode = {
    val encodedName = ci.encodedName
    new ClassNode(
      encodedName,
      decodeClassName(encodedName),
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
      displayName(encodedName),
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
