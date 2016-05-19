package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode, Node}
import org.singlespaced.d3js.{Link, forceModule}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters.genTravConvertible2JSRichGenTrav

final case class GraphNode[N <: Node](displayName: String, data: N) extends forceModule.Node

final case class GraphLink(source: GraphNode[_], target: GraphNode[_]) extends Link[GraphNode[_]]

object D3GraphController {

  private var callGraph = CallGraph()

  def init(data: Data, graph: CallGraph): Unit = {
    callGraph = graph
    callGraph.classes.withFilter(_.isExported) foreach { c =>
      val node = createClassNode(data, c)
      c.methods foreach expand(data, node)
    }
  }

  private def filter(className: String, methodName: String): Set[MethodNode] = {
    callGraph.classes.withFilter(_.encodedName == className)
      .flatMap(_.methods.filter(_.encodedName == methodName))
  }

  private def createClassNode(data: Data, classNode: ClassNode): GraphNode[ClassNode] = {
    val displayName = Decoder.decodeClass(classNode.encodedName)
    val node = GraphNode(displayName, classNode)
    data.addNode(node)
    node
  }

  private def createMethodNode(data: Data, methodNode: MethodNode): GraphNode[MethodNode] = {
    val displayName = Decoder.decodeMethod(methodNode.className, methodNode.encodedName)
    val node = GraphNode(displayName, methodNode)
    data.addNode(node)
    node
  }

  private def expand[N <: Node](data: Data, source: GraphNode[N])(methodNode: MethodNode): Option[GraphNode[N]] = {
    val node = createMethodNode(data, methodNode)
    val link = GraphLink(source, node)
    if (data.addLink(link)) Some(node) else None
  }

  def expandAll(data: Data, source: GraphNode[MethodNode]): Unit = for {
    (className, methods) <- source.data.calledFrom
    methodName <- methods
  } filter(className, methodName) foreach expand(data, source)


  def expandRecursive(data: Data, source: GraphNode[MethodNode]): Unit = for {
    (className, methods) <- source.data.calledFrom.lastOption
    methodName <- methods.lastOption
    methodNode <- filter(className, methodName).lastOption
    newMethodNode <- expand(data, source)(methodNode)
  } expandRecursive(data, newMethodNode)

  def expandRecursive(data: Data, encodedName: String): Unit = {
    val Array(className, methodName) = encodedName.split('.').take(2)
    filter(className, methodName) foreach { n =>
      val node = createMethodNode(data, n)
      expandRecursive(data, node)
    }
  }

  def search(className: String, methodName: String, isExported: Boolean): Traversable[(String, String)] = {
    def exported(m: MethodNode) = !isExported || m.isExported
    callGraph.classes.withFilter(_.encodedName.contains(className))
      .flatMap(_.methods.filter(m => m.encodedName.contains(methodName) && exported(m))) map { methodNode =>
        val displayName = Decoder.decodeMethod(methodNode.className, methodNode.encodedName)
        val encodedName = methodNode.className + "." + methodNode.encodedName
      (displayName, encodedName)
    }
  }

  class Data {
    private val nodes = mutable.Set[GraphNode[_ <: Node]]()
    private val links = mutable.Set[GraphLink]()

    def getNodes: js.Array[GraphNode[_ <: Node]] = nodes.toJSArray
    def getLinks: js.Array[GraphLink] = links.toJSArray

    def addNode(node: GraphNode[_ <: Node]): Boolean = nodes.add(node)
    def addLink(link: GraphLink): Boolean = links.add(link)
  }

}
