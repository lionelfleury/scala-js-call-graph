package ch.epfl.callgraph.utils

import upickle.Js
import upickle.default._

object Utils {

  sealed trait Node {
    def encodedName: String
    def isExported: Boolean
    def nonExistent: Boolean
    def isReachable: Boolean
  }

  sealed trait ErrorInfo {
    def from: String
  }

  @key("C")
  final case class ClassNode(encodedName: String,
                             isExported: Boolean,
                             nonExistent: Boolean,
                             isReachable: Boolean,
                             superClass: Option[String],
                             interfaces: Seq[String],
                             methods: Seq[MethodNode]) extends Node

  @key("M")
  final case class MethodNode(encodedName: String,
                              isExported: Boolean,
                              nonExistent: Boolean,
                              isReachable: Boolean,
                              className: String,
                              methodsCalled: Map[String, Seq[String]],
                              calledFrom: Map[String, Seq[String]]) extends Node

  @key("MM")
  final case class MissingMethodInfo(encodedName: String, className: String, from: String) extends ErrorInfo

  @key("MC")
  final case class MissingClassInfo(encodedName: String, from: String) extends ErrorInfo

  final case class CallGraph(classes: Seq[ClassNode] = Seq.empty, errors: Seq[ErrorInfo] = Seq.empty)

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
        ("re", upickle.default.writeJs[Boolean](t.isReachable)),
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
      (_, reachable),
      (_, className),
      (_, methodsCalled),
      (_, calledFrom)
      ) => new MethodNode(upickle.default.readJs[String](encodedName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[Boolean](nonExistent),
        upickle.default.readJs[Boolean](reachable),
        upickle.default.readJs[String](className),
        upickle.default.readJs[Map[String, Seq[String]]](methodsCalled),
        upickle.default.readJs[Map[String, Seq[String]]](calledFrom)
      )
    }
  }

  object ClassNode {

    implicit val methodNodeWriter = upickle.default.Writer[ClassNode] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
        ("i", upickle.default.writeJs[Boolean](t.isExported)),
        ("ne", upickle.default.writeJs[Boolean](t.nonExistent)),
        ("re", upickle.default.writeJs[Boolean](t.isReachable)),
        ("s", upickle.default.writeJs[Option[String]](t.superClass)),
        ("in", upickle.default.writeJs[Seq[String]](t.interfaces)),
        ("m", upickle.default.writeJs[Seq[MethodNode]](t.methods))
      )
    }

    implicit val methodNodeReader = upickle.default.Reader[ClassNode] {
      case Js.Obj(
      (_, encodedName),
      (_, isExported),
      (_, nonExistent),
      (_, reachable),
      (_, superClass),
      (_, interfaces),
      (_, methods)
      ) => new ClassNode(upickle.default.readJs[String](encodedName),
        upickle.default.readJs[Boolean](isExported),
        upickle.default.readJs[Boolean](nonExistent),
        upickle.default.readJs[Boolean](reachable),
        upickle.default.readJs[Option[String]](superClass),
        upickle.default.readJs[Seq[String]](interfaces),
        upickle.default.readJs[Seq[MethodNode]](methods)
      )
    }
  }

  object MissingMethodInfo {
    implicit val missingMethodWriter = upickle.default.Writer[MissingMethodInfo] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
        ("c", Js.Str(t.className)),
        ("f", Js.Str(t.from))
      )
    }

    implicit val missingMethodReader = upickle.default.Reader[MissingMethodInfo] {
      case Js.Obj(
      (_, encodedName),
      (_, className),
      (_, from)
      ) => new MissingMethodInfo(
        upickle.default.readJs[String](encodedName),
        upickle.default.readJs[String](className),
        upickle.default.readJs[String](from)
      )
    }
  }

  object MissingClassInfo {
    implicit val missingClassWriter = upickle.default.Writer[MissingClassInfo] {
      case t => Js.Obj(
        ("e", Js.Str(t.encodedName)),
        ("f", Js.Str(t.from))
      )
    }

    implicit val missingClassReader = upickle.default.Reader[MissingClassInfo] {
      case Js.Obj(
      (_, encodedName),
      (_, from)
      ) => new MissingClassInfo(
        upickle.default.readJs[String](encodedName),
        upickle.default.readJs[String](from)
      )
    }
  }
}
