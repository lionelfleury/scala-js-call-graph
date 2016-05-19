package ch.epfl.callgraph.utils

import upickle.Js
import upickle.default._

object Utils {

  sealed trait Node {
    val encodedName: String
    val isExported: Boolean
    val nonExistent: Boolean
  }

  @key("C")
  final case class ClassNode(encodedName: String,
                             isExported: Boolean,
                             nonExistent: Boolean,
                             superClass: String,
                             interfaces: Seq[String],
                             methods: Set[MethodNode]) extends Node

  @key("M")
  final case class MethodNode(encodedName: String,
                              isExported: Boolean,
                              nonExistent: Boolean,
                              className: String,
                              methodsCalled: Map[String, List[String]],
                              calledFrom: Map[String, List[String]]) extends Node

  final case class CallGraph(classes: Set[ClassNode] = Set.empty)

  /**
    * Unfortunately there is an issue with uPickle on Scala 2.10.
    * Plugins uses 2.10 to compile, forcing us to write an explicit
    * converter.
    * See https://github.com/lihaoyi/upickle-pprint/issues/20
    * I couldn't make quasiquotes work with our project...
    */
  object MethodNode {

    implicit val methodNodeWriter = upickle.default.Writer[MethodNode] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
        ("i", upickle.default.writeJs[Boolean](t.isExported)),
        ("ne", upickle.default.writeJs[Boolean](t.nonExistent)),
        ("c", Js.Str(t.className)),
        ("m", upickle.default.writeJs(t.methodsCalled)),
        ("cf", upickle.default.writeJs(t.calledFrom))
      )
    }

    implicit val methodNodeReader = upickle.default.Reader[MethodNode] {
      case Js.Obj(
        (_, encodedName),
        (_, isExported),
        (_, nonExistent),
        (_, className),
        (_, methodsCalled),
        (_, calledFrom)
      ) => new MethodNode(
        upickle.default.readJs[String](encodedName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[Boolean](nonExistent),
        upickle.default.readJs[String](className),
        upickle.default.readJs[Map[String, List[String]]](methodsCalled),
        upickle.default.readJs[Map[String, List[String]]](calledFrom)
      )
    }
  }

  object ClassNode {

    implicit val methodNodeWriter = upickle.default.Writer[ClassNode] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
        ("i", upickle.default.writeJs[Boolean](t.isExported)),
        ("ne", upickle.default.writeJs[Boolean](t.nonExistent)),
        ("s", upickle.default.writeJs[String](t.superClass)),
        ("in", upickle.default.writeJs[Seq[String]](t.interfaces)),
        ("m", upickle.default.writeJs[Set[MethodNode]](t.methods))
      )
    }

    implicit val methodNodeReader = upickle.default.Reader[ClassNode] {
      case Js.Obj(
        (_, encodedName),
        (_, isExported),
        (_, nonExistent),
        (_, superClass),
        (_, interfaces),
        (_, methods)
      ) => new ClassNode(
        upickle.default.readJs[String](encodedName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[Boolean](nonExistent),
        upickle.default.readJs[String](superClass),
        upickle.default.readJs[Seq[String]](interfaces),
        upickle.default.readJs[Set[MethodNode]](methods)
      )
    }
  }

}
