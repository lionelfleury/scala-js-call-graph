package ch.epfl.callgraph.visualization.model

import ch.epfl.callgraph.utils.Utils._
import org.scalajs.core.ir.Definitions
import org.scalajs.core.ir.Types.{ArrayType, ClassType, ReferenceType}

/**
  * Created by Lionel on 07/05/16.
  */
object Decoder {

  /**
    * Return a decoded method name, if the method is exported, will show it between <>
    *
    * @param encodedName the name of the method to decode
    * @return the decoded name
    */
  def decodeMethodName(encodedName: String): String = {
    val simpleName = Definitions.decodeMethodName(encodedName)._1
    val exportedPrefix = "$$js$exported$meth$"

    if (simpleName.startsWith(exportedPrefix)) "<" + simpleName.replace(exportedPrefix, "") + ">"
    else simpleName
  }

  /**
    * Split a fully encoded name (class + method)
    *
    * @param fullEncodedName the full name to decode
    * @return a tuple containing the class and the method name
    */
  def splitEncodedName(fullEncodedName: String): (String, String) = {
    val as = fullEncodedName.split('.')
    val className = as(0)
    val methodName = if (as.length > 1) as(1) else ""
    (className, methodName)
  }

  /**
    * Create a full encoded name (class + method)
    *
    * @param className  the name of the class
    * @param methodName the name of the method
    * @return the concatenation of both arguments
    */
  def getFullEncodedName(className: String, methodName: String): String = className + "." + methodName

  /**
    * Returns the fully encoded name, works on class and methods
    *
    * @param node
    * @return the full encoded name of the node
    */
  def getFullEncodedName(node: Node): String = node match {
    case classNode: ClassNode => classNode.encodedName
    case methodNode: MethodNode => getFullEncodedName(methodNode.className, methodNode.encodedName)
  }

  /**
    * Return the fully encoded name, but for ErrorInfo
    *
    * @param error the error to look into
    * @return the full encoded name of the error target
    */
  def getFullEncodedName(error: ErrorInfo): String = error match {
    case missingClass: MissingClassInfo => missingClass.encodedName
    case missingMethod: MissingMethodInfo => getFullEncodedName(missingMethod.className, missingMethod.encodedName)
  }

  /**
    * Create the display name of the node
    * Remove useless data from method (argument type and return type)
    *
    * @param node the targeted node
    * @return the display name
    */
  def getDisplayName(node: Node): String = node match {
    case classNode: ClassNode => Definitions.decodeClassName(classNode.encodedName)
    case methodNode: MethodNode =>
      val displayName =
        if (methodNode.isExported) methodNode.encodedName
        else decodeMethodName(methodNode.encodedName)

      Definitions.decodeClassName(methodNode.className) + "." + displayName
  }

  /**
    * Return the display name of the target of an error
    *
    * @param error the target error
    * @return the display name of the error's target
    */
  def getDisplayName(error: ErrorInfo): String = error match {
    case missingClass: MissingClassInfo =>
      Definitions.decodeClassName(missingClass.encodedName)
    case missingMethod: MissingMethodInfo =>
      val displayName = decodeMethodName(missingMethod.encodedName)
      Definitions.decodeClassName(missingMethod.className) + "." + displayName
  }

  /**
    * Shorten the package name out of a display name
    *
    * @param displayName the display name to shorten
    * @return Shortened name (one letter per package)
    */
  def shortenDisplayName(displayName: String): String = {
    val as = displayName.split('.')
    for (i <- 0 until as.length - 2) as(i) = as(i).take(1)
    as.mkString(".")
  }

  def shortenDisplayName(node: Node): String = shortenDisplayName(getDisplayName(node))

  /**
    * The full name of the node, including the parameters type and the return one
    *
    * @param node the target node
    * @return the full name
    */
  def fullDisplayName(node: Node): String = {
    node match {
      case node: MethodNode => {
        if (node.isExported) getDisplayName(node)
        else {
          val (simpleName, paramTypes, resultType) = org.scalajs.core.ir.Definitions.decodeMethodName(node.encodedName)
          Definitions.decodeClassName(node.className) + simpleName + "(" + paramTypes.map(typeDisplayName _).mkString(",") + ")" +
            resultType.fold("")(typeDisplayName)
        }
      }
      case node: ClassNode => getDisplayName(node)
    }
  }

  def fullDisplayName(error: MissingMethodInfo) = {
    val (simpleName, paramTypes, resultType) = org.scalajs.core.ir.Definitions.decodeMethodName(error.encodedName)
    Definitions.decodeClassName(error.className) + simpleName + "(" + paramTypes.map(typeDisplayName _).mkString(",") + ")" + resultType.fold("")(typeDisplayName)
  }

  private def typeDisplayName(tpe: ReferenceType): String = tpe match {
    case ClassType(encodedName) => Definitions.decodeClassName(encodedName)
    case ArrayType(base, dimensions) => "[" * dimensions + Definitions.decodeClassName(base)
  }


}