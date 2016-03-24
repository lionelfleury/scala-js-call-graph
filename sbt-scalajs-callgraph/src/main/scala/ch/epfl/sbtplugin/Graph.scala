package ch.epfl.sbtplugin

import java.io.{FileWriter, BufferedWriter, File}

import ch.epfl.callgraph.utils.Utils.{ClassNode, MethodNode, Node}
import org.scalajs.core.tools.linker.analyzer.Analysis
import org.scalajs.core.tools.linker.analyzer.Analysis.{FromMethod, ClassInfo}
import upickle.Js
import upickle.default._

import scala.language.implicitConversions

class Graph(graph: Map[String, Analysis.ClassInfo]) {

  implicit def toClassNode(classInfo: Analysis.ClassInfo): ClassNode = {
    new ClassNode(
      classInfo.encodedName,
      classInfo.displayName,
      classInfo.nonExistent,
      (classInfo.methodInfos ++ classInfo.staticMethodInfos).map(_._2.encodedName).toSet
    )
  }

  implicit def toMethodNode(methodInfo: Analysis.MethodInfo): MethodNode = {
    new MethodNode(
      methodInfo.encodedName,
      methodInfo.displayName,
      methodInfo.nonExistent,
      methodInfo.owner.encodedName,
      methodInfo.isStatic
    )
  }

  val newGraph = collection.mutable.Map[String, Node]()

  /**
    * Add all the methods of the given class to the graph
 *
    * @param classInfo The class to add the methods from
    */
  def addMissingMethods(classInfo: ClassInfo) = {
    classInfo.methodInfos ++ classInfo.staticMethodInfos foreach {
      /** Add all methods to graph **/
      case (_, meth) =>
        if(!newGraph.isDefinedAt(meth.encodedName))
          newGraph += (meth.encodedName -> meth)
      case _ =>
    }
  }

  /*
    Create the new graph to be exported.
    In this graph, methods and classes are on the same level.
   */
  graph foreach {
    case (name: String, vertex: Analysis.ClassInfo) => {
      newGraph.get(vertex.encodedName) match {
        case None => {
          newGraph += (vertex.encodedName -> vertex)
        }
        case Some(_) =>
      }
      addMissingMethods(vertex)

      /*
        Inverse the existing edges
       */
      vertex.instantiatedFrom.foreach {
        case f: FromMethod => {
          val method = f.methodInfo
          val methodNode: MethodNode = newGraph.get(method.encodedName) match {
            case Some(x: MethodNode) => x /** Found it **/
            case Some(x) => throw new Exception("Should be a  MethodNode") /** **/
            case None => {
              /** Not in the graph **/
              val methodNode: MethodNode = method
              newGraph += (method.encodedName -> methodNode)
              methodNode
            }
          }
          methodNode.out += vertex.encodedName /** Add a link from the method to the class **/
        }
        case _ => /** Not implemented yet **/
      }
    }
  }

  val t = newGraph.map(x => x._2)
  val file = new File("graph.json")
  val bw = new BufferedWriter(new FileWriter(file))
  bw.write(write(t))
  bw.close()

}
