package ch.epfl.callgraph.utils

import upickle.default._

object Utils {

  sealed trait Node {
    val encodedName: String
    val isExported: Boolean
  }

  @key("Class")
  case class ClassNode(encodedName: String,
                       isExported: Boolean,
                       superClass: Option[String],
                       interfaces: List[String],
                       methods: List[String]) extends Node

  @key("Method")
  case class MethodNode(encodedName: String,
                        isExported: Boolean,
                        methodsCalled: Map[String, List[String]],
                        instantiatedClasses: List[String]) extends Node

}
