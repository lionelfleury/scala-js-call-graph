package ch.epfl.callgraph.utils

import upickle.Js
import upickle.default._

object Utils {

  sealed trait Node {
    val encodedName: String
//    val displayName: String
    val isExported: Boolean
  }

  @key("M")
  case class MethodNode(encodedName: String,
//                        displayName: String,
                        isExported: Boolean,
                        className:  String,
                        methodsCalled: Map[String, List[String]],
                        instantiatedClasses: List[String]) extends Node

  @key("C")
  case class ClassNode(encodedName: String,
//                       displayName: String,
                       isExported: Boolean,
                       superClass: Option[String],
                       interfaces: Seq[String],
                       methods: Set[MethodNode]) extends Node

  /**
    * Unfortunately there is an issue with uPickle on Scala 2.10.
    * Plugins uses 2.10 to compile, forcing us to write an explicit
    * converter.
    * See https://github.com/lihaoyi/upickle-pprint/issues/20
    * I couldn't make quasiquotes work with our project...
    */
  import upickle.Js
  object MethodNode {
    implicit val methodNodeWriter = upickle.default.Writer[MethodNode] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
//        ("displayName", Js.Str(t.displayName)),
        ("i", upickle.default.writeJs[Boolean](t.isExported)),
        ("c", Js.Str(t.className)),
        ("m", upickle.default.writeJs(t.methodsCalled)),
        ("ic", upickle.default.writeJs(t.instantiatedClasses))
      )
    }
    implicit val methodNodeReader = upickle.default.Reader[MethodNode] {
      case Js.Obj(
      (_, encodedName),
//      (_, displayName),
      (_, isExported),
      (_, className),
      (_, methodsCalled),
      (_, instantiatedClasses)
      ) => new MethodNode(upickle.default.readJs[String](encodedName),
//        upickle.default.readJs[String](displayName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[String](className),
        upickle.default.readJs[Map[String, List[String]]](methodsCalled),
        upickle.default.readJs[List[String]](instantiatedClasses)
      )
    }
  }

  object ClassNode {
    implicit val methodNodeWriter = upickle.default.Writer[ClassNode] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
//        ("displayName", Js.Str(t.displayName)),
        ("i", upickle.default.writeJs[Boolean](t.isExported)),
        ("s", upickle.default.writeJs[Option[String]](t.superClass)),
        ("in", upickle.default.writeJs[Seq[String]](t.interfaces)),
        ("m", upickle.default.writeJs[Set[MethodNode]](t.methods))
      )
    }
    implicit val methodNodeReader = upickle.default.Reader[ClassNode] {
      case Js.Obj(
      (_, encodedName),
//      (_, displayName),
      (_, isExported),
      (_, superClass),
      (_, interfaces),
      (_, methods)
      ) => new ClassNode(upickle.default.readJs[String](encodedName),
//        upickle.default.readJs[String](displayName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[Option[String]](superClass),
        upickle.default.readJs[Seq[String]](interfaces),
        upickle.default.readJs[Set[MethodNode]](methods)
      )
    }
  }

  final case class CallGraph(classes: Set[ClassNode])
}
