package ch.epfl.callgraph.visualization.model

import ch.epfl.callgraph.utils.Utils.{ClassNode, MethodNode, Node}

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

  def decodeMethodName(encodedName: String): String = {
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

    val exportedPrefix = "$$js$exported$meth$"
    if (simpleName.startsWith(exportedPrefix)) "<" + simpleName.replace(exportedPrefix, "") + ">"
    else simpleName
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

  def getDisplayName(node: Node): String = node match {
    case classNode: ClassNode => decodeClassName(classNode.encodedName)
    case methodNode: MethodNode => decodeClassName(methodNode.className) + "." + decodeMethodName( methodNode.encodedName)
  }

}