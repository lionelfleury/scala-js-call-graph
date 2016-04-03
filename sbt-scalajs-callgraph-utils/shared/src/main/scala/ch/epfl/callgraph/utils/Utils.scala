package ch.epfl.callgraph.utils

import upickle.default._

object Utils {

  sealed trait Node {
    val encodedName: String
    val displayName: String
    val isExported: Boolean
  }

  @key("Class")
  case class ClassNode(encodedName: String,
                       displayName: String,
                       isExported: Boolean,
                       superClass: Option[String],
                       interfaces: Seq[String],
                       methods: Set[String]) extends Node

  @key("Method")
  case class MethodNode(encodedName: String,
                        displayName: String,
                        isExported: Boolean,
                        methodsCalled: Set[String],
                        instantiatedClasses: List[String]) extends Node

  final case class CallGraph(classes: Set[ClassNode], methods: Set[MethodNode])

}
