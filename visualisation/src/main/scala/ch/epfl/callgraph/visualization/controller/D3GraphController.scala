package ch.epfl.callgraph.visualization.controller

import ch.epfl.callgraph.utils.Utils.{CallGraph, ClassNode, MethodNode}
import ch.epfl.callgraph.visualization.model.D3GraphModel._
import ch.epfl.callgraph.visualization.model.Decoder

object D3GraphController {

  private var callGraph: CallGraph = CallGraph()

  private def layer: Layer = Layers.current

  /**
    * Initialize the controller
    *
    * @param graph the callGraph
    */
  def init(graph: CallGraph): Unit = {
    Layers.reset
    callGraph = graph
    callGraph.classes.find(_.isExported) foreach { c =>
      val source = layer.addNode(c)
      expandAllFrom(source)
    }
    layer.update()
  }

  /**
    * Initialize a new layer and open it
    *
    * @param fullEncodedName the full encoded name of the node
    */
  def initNewLayer(fullEncodedName: String) = {
    Layers.addLayer()
    D3GraphController.expandRecursive(fullEncodedName)
  }

  /**
    * Expand all nodes pointing to this node
    *
    * @param target the target node
    */
  def expandAllTo(target: GraphNode): Unit = target.data match {
    case methodNode: MethodNode => for {
      (className, methods) <- methodNode.calledFrom
      methodName <- methods
    } filter(className, methodName) foreach expandTo(target)
      layer.update()
    case _ => // nothing to do...
  }

  private def expandTo(target: GraphNode)(source: MethodNode): Option[GraphNode] = {
    val src = layer.addNode(source)
    val added = layer.addLink(src, target)
    if (added) Some(src) else None
  }

  /**
    * Expand all the methods from this node
    *
    * @param source the source node
    */
  def expandAllFrom(source: GraphNode) = source.data match {
    case methodNode: MethodNode =>
      for {
        (className, methods) <- methodNode.methodsCalled
        methodName <- methods
      } filter(className, methodName) foreach expandFrom(source)
      layer.update()
    case classNode: ClassNode => classNode.methods foreach expandFrom(source)
  }

  private def expandFrom(source: GraphNode)(target: MethodNode): Option[GraphNode] = {
    val tgt = layer.addNode(target)
    val added = layer.addLink(source, tgt)
    if (added) Some(tgt) else None
  }

  /**
    * Expand a full path pointing to this node
    *
    * @param target the target node
    */
  def expandRecursive(target: GraphNode): Unit = target.data match {
    case methodNode: MethodNode =>
      for {
        (className, methods) <- methodNode.calledFrom.lastOption
        methodName <- methods.lastOption
        methodNode <- filter(className, methodName).lastOption
        newMethodNode <- expandTo(target)(methodNode)
      } expandRecursive(newMethodNode)

      layer.update()
    case _ => // TODO: classNode
  }

  /**
    * Expand a full path pointing to this node
    *
    * @param fullEncodedName the full encoded name of the target node
    */
  def expandRecursive(fullEncodedName: String): Unit = {
    val (className, methodName) = Decoder.splitEncodedName(fullEncodedName)
    filter(className, methodName) foreach { n =>
      val node = layer.addNode(n)
      expandRecursive(node)
    }
  }

  /**
    * Hide a node and all its related links
    *
    * @param node the node to be hidden
    */
  def hideNode(node: GraphNode) = {
    layer.removeNode(node)
    layer.update()
  }

  /**
    * Search the call graph for a given sequence of partial keywords
    *
    * @param text        the partial keywords
    * @param isExported  true for exported nodes only
    * @param isReachable true for reachable nodes only
    * @return a sequence of (displayName, encodedName)
    */
  def search(text: Seq[String], isExported: Boolean, isReachable: Boolean): Seq[(String, String, String)] = {
    def exported(m: MethodNode) = !isExported || m.isExported
    def reachable(m: MethodNode) = !isReachable || m.isReachable
    (for {
      classNode <- callGraph.classes
      methodNode <- classNode.methods if exported(methodNode) && reachable(methodNode)
      dn = Decoder.fullDisplayName(methodNode)
      displayName = Decoder.getDisplayName(methodNode)
      encodedName = Decoder.getFullEncodedName(methodNode)
      shortName = Decoder.shortenDisplayName(displayName)
      if text.forall(displayName.contains)
    } yield (shortName, dn, encodedName)) (collection.breakOut).sorted
  }

  private def filter(className: String, methodName: String): Seq[MethodNode] = {
    if (className == "core") {
      Seq[MethodNode]()
    } else if (className == "exports") {
      Seq[MethodNode]()
    } else {
      callGraph.classes.withFilter(_.encodedName == className)
        .flatMap(_.methods.filter(_.encodedName == methodName))
    }
  }
}
