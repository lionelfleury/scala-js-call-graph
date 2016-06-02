package ch.epfl.callgraph.visualization.model

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions
import org.scalajs.core.ir.Types.{ArrayType, ClassType, ReferenceType}

/**
  * Created by Lionel on 07/05/16.
  */
object Decoder {

  def decodeClassName(encodedName: String) : String = Definitions.decodeClassName(encodedName)

  def decodeMethodName(encodedName: String): String = Definitions.decodeMethodName(encodedName)._1

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