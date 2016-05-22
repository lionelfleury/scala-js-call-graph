package ch.epfl.callgraph.visualization.controller

import ch.epfl.callgraph.utils.Utils.{CallGraph, MethodNode}
import ch.epfl.callgraph.visualization.model.D3GraphModel._
import ch.epfl.callgraph.visualization.model.Decoder

object D3GraphController {

  private var callGraph: CallGraph = CallGraph()

  private def layer: Layer = Layers.current

  def init(graph: CallGraph): Unit = {
    callGraph = graph
    callGraph.classes.withFilter(_.isExported) foreach { c =>
      val source = layer.addNode(c)
      c.methods foreach expandBackwards(source)
    }
    layer.update()
  }

  def initNewLayer(fullEncodedName: String) = {
    Layers.addLayer()
    D3GraphController.expandRecursive(fullEncodedName)
  }


  def expandAllTowards(target: GraphNode): Unit = target.data match {
    case methodNode: MethodNode => for {
      (className, methods) <- methodNode.calledFrom
      methodName <- methods
    } filter(className, methodName) foreach expandForward(target)
      layer.update()
    case _ => // TODO: classNode
  }

  private def expandBackwards(source: GraphNode)(t: MethodNode): Option[GraphNode] = {
    val target = layer.addNode(t)
    val added = layer.addLink(source, target)
    if (added) Some(target) else None
  }

  private def expandForward(target: GraphNode)(s: MethodNode): Option[GraphNode] = {
    val source = layer.addNode(s)
    val added = layer.addLink(source, target)
    if (added) Some(source) else None
  }

  def expandAllBackwards(source: GraphNode) = source.data match {
    case methodNode: MethodNode =>
      for ((cn, mns) <- methodNode.methodsCalled; mn <- mns) filter(cn, mn) foreach expandBackwards(source)
      layer.update()
    case _ => // TODO: classNode
  }

  def expandRecursive(source: GraphNode): Unit = source.data match {
    case methodNode: MethodNode => for {
      (className, methods) <- methodNode.calledFrom.lastOption
      methodName <- methods.lastOption
      methodNode <- filter(className, methodName).lastOption
      newMethodNode <- expandForward(source)(methodNode)
    } expandRecursive(newMethodNode)
      layer.update()
    case _ => // TODO: classNode
  }

  def expandRecursive(fullEncodedName: String): Unit = {
    val (className, methodName) = Decoder.splitEncodedName(fullEncodedName)
    filter(className, methodName) foreach { n =>
      val node = layer.addNode(n)
      expandRecursive(node)
    }
  }

  def hideNode(node: GraphNode) = {
    layer.removeNode(node)
    layer.update()
  }

  def search(text: Seq[String], isExported: Boolean): Traversable[(String, String)] = {
    def exported(m: MethodNode) = !isExported || m.isExported
    for {
      classNode <- callGraph.classes
      methodNode <- classNode.methods if exported(methodNode)
      displayName = Decoder.getDisplayName(methodNode)
      encodedName = Decoder.getFullEncodedName(methodNode)
      if text.forall(displayName.contains)
    } yield (displayName, encodedName)
  }

  private def filter(className: String, methodName: String): Set[MethodNode] = {
    callGraph.classes.withFilter(_.encodedName == className)
      .flatMap(_.methods.filter(_.encodedName == methodName))
  }
}
