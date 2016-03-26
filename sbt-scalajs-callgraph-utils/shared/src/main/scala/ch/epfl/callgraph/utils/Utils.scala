package ch.epfl.callgraph.utils

import upickle.Js

import scala.collection.mutable
import scala.language.implicitConversions

object Utils {

  // TODO : refactor those with the new ir.Infos classes
  sealed trait Node {
    val encodedName: String
    val displayName: String
    val nonExistent: Boolean
    val out = mutable.Set[String]()
    val in = mutable.Set[String]()
  }

  case class ClassNode(encodedName: String,
                       displayName: String,
                       nonExistent: Boolean,
                       methods: Set[String]) extends Node

  case class MethodNode(encodedName: String,
                        displayName: String,
                        nonExistent: Boolean,
                        parent: String,
                        static: Boolean) extends Node

  implicit private[this] def toJsBool(b: Boolean): Js.Value = b match {
    case true => Js.True
    case false => Js.False
  }

  implicit private[this] def boolToJs(b: Js.Value): Boolean = b match {
    case Js.True => true
    case Js.False => false
    case _ => throw new Exception("Boolean expected")
  }

  object ClassNode {

    implicit val thing2Writer = upickle.default.Writer[ClassNode] {
      case t =>
        val outEdges: Array[Js.Value] = t.out.map(Js.Str).toArray[Js.Value]
        val inEdges: Array[Js.Value] = t.in.map(Js.Str).toArray[Js.Value]
        val methods: Array[Js.Value] = t.methods.map(Js.Str).toArray[Js.Value]

        Js.Obj(
          ("encodedName", Js.Str(t.encodedName)),
          ("displayName", Js.Str(t.displayName)),
          ("nonExistent", t.nonExistent),
          ("outEdges", Js.Arr(outEdges: _*)),
          ("inEdges", Js.Arr(inEdges: _*)),
          ("methods", Js.Arr(methods: _*)),
          ("type", Js.Str("class"))
        )
    }

    implicit val thing2Reader = upickle.default.Reader[ClassNode] {
      case Js.Obj((_, encodedName), (_, displayName), (_, nonExistent), (_, out: Js.Arr), (_, in: Js.Arr), (_, methods: Js.Arr), _) =>
        val node = new ClassNode(encodedName.toString, displayName.toString, nonExistent, methods.value.map(_.toString).toSet)
        node.out ++= out.value.map(_.toString)
        node.in ++= in.value.map(_.toString)
        node
    }
  }

  object MethodNode {

    implicit val thing2Writer = upickle.default.Writer[MethodNode] {
      case t =>
        val outEdges: Array[Js.Value] = t.out.map(Js.Str).toArray[Js.Value]
        val inEdges: Array[Js.Value] = t.in.map(Js.Str).toArray[Js.Value]
        Js.Obj(
          ("encodedName", Js.Str(t.encodedName)),
          ("displayName", Js.Str(t.displayName)),
          ("nonExistent", t.nonExistent),
          ("parent", Js.Str(t.parent)),
          ("static", t.static),
          ("outEdges", Js.Arr(outEdges: _*)),
          ("inEdges", Js.Arr(inEdges: _*)),
          ("type", Js.Str("method"))
        )
    }

    implicit val thing2Reader = upickle.default.Reader[MethodNode] {
      case Js.Obj((_, encodedName), (_, displayName), (_, nonExistent), (_, parent), (_, static), (_, out: Js.Arr), (_, in: Js.Arr), _) =>
        val node = new MethodNode(encodedName.toString, displayName.toString, nonExistent, parent.toString, static)
        node.out ++= out.value.map(_.toString)
        node.in ++= in.value.map(_.toString)
        node
    }
  }

}
