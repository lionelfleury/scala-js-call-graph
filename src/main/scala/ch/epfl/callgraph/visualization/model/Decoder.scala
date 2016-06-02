package ch.epfl.callgraph.visualization.model

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Types.{ArrayType, ClassType, ReferenceType}

/**
  * Created by Lionel on 07/05/16.
  */
object Decoder {

  def decodeClassName(encodedName: String): String = {
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
      yield s"scala_Tuple$index" -> ("T" + index)
    ) ++ (
    for (index <- 0 to 22)
      yield s"scala_Function$index" -> ("F" + index)
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

  private def isConstructorName(name: String): Boolean =
    name.startsWith("init___")

//  def decodeReferenceType(encodedName: String): ReferenceType = {
//    val arrayDepth = encodedName.indexWhere(_ != 'A')
//    if (arrayDepth == 0)
//      ClassType(encodedName)
//    else
//      ArrayType(encodedName.substring(arrayDepth), arrayDepth)
//  }

  def decodeMethodName(encodedName: String): String = {
    val (simpleName, privateAndSigString) = if (isConstructorName(encodedName)) {
      val privateAndSigString =
        if (encodedName == "init___") ""
        else encodedName.stripPrefix("init___") + "__"
      ("<init>", privateAndSigString)
    } else {
      val pos = encodedName.indexOf("__")
      val pos2 =
        if (!encodedName.substring(pos + 2).startsWith("p")) pos
        else encodedName.indexOf("__", pos + 2)
      (encodedName.substring(0, pos), encodedName.substring(pos2 + 2))
    }

    // -1 preserves trailing empty strings
    val parts = privateAndSigString.split("__", -1).toSeq
    val paramsAndResultStrings =
      if (parts.headOption.exists(_.startsWith("p"))) parts.tail
      else parts

    val paramStrings :+ resultString = paramsAndResultStrings

//    val paramTypes = paramStrings.map(decodeReferenceType).toList
//    val resultType =
//      if (resultString == "") None // constructor or reflective proxy
//      else Some(decodeReferenceType(resultString))

    val exportedPrefix = "$$js$exported$meth$"
    val newSimpleName =
      if (simpleName.startsWith(exportedPrefix)) "<" + simpleName.replace(exportedPrefix, "") + ">"
      else simpleName

//    def typeDisplayName(tpe: ReferenceType): String = tpe match {
//      case ClassType(encodedName) => decodeClassName(encodedName)
//      case ArrayType(base, dimensions) => "[" * dimensions + decodeClassName(base)
//    }

    newSimpleName /* + "(" + paramTypes.map(typeDisplayName).mkString(",") + ")" +
      resultType.fold("")(typeDisplayName) */
  }

  def splitEncodedName(fullEncodedName: String): (String, String) = {
    val as = fullEncodedName.split('.')
    val className = as(0)
    val methodName = if (as.length > 1) as(1) else ""
    (className, methodName)
  }

  def getFullEncodedName(className: String, methodName: String): String = className + "." + methodName

  def getFullEncodedName(node: Node): String = node match {
    case classNode: ClassNode => classNode.encodedName
    case methodNode: MethodNode => getFullEncodedName(methodNode.className, methodNode.encodedName)
  }

  def getFullEncodedName(error: ErrorInfo): String = error match {
    case missingClass: MissingClassInfo => missingClass.encodedName
    case missingMethod: MissingMethodInfo => getFullEncodedName(missingMethod.className, missingMethod.encodedName)
  }

  def getDisplayName(node: Node): String = node match {
    case classNode: ClassNode => decodeClassName(classNode.encodedName)
    case methodNode: MethodNode =>
      val displayName =
        if (methodNode.isExported) methodNode.encodedName
        else decodeMethodName(methodNode.encodedName)

      decodeClassName(methodNode.className) + "." + displayName
  }

  def getDisplayName(error: ErrorInfo): String = error match {
    case missingClass: MissingClassInfo => decodeClassName(missingClass.encodedName)
    case missingMethod: MissingMethodInfo => decodeClassName(missingMethod.className) + "." + missingMethod.encodedName
  }

  def shortenDisplayName(displayName: String): String = {
    val as = displayName.split('.')
    for (i <- 0 until as.length - 2) as(i) = as(i).take(1)
    as.mkString(".")
  }

  def shortenDisplayName(node: Node): String = shortenDisplayName(getDisplayName(node))

}