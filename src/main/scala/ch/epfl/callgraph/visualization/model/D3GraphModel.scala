package ch.epfl.callgraph.visualization.model

import ch.epfl.callgraph.utils.Utils.Node
import org.singlespaced.d3js.{Link, forceModule}

object D3GraphModel {

  final case class GraphNode(data: Node) extends forceModule.Node

  final case class GraphLink(source: GraphNode, target: GraphNode) extends Link[GraphNode]

}
