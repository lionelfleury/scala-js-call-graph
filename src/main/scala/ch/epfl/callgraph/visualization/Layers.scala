package ch.epfl.callgraph.visualization

import ch.epfl.callgraph.utils.Utils.{ClassNode, MethodNode}
import org.scalajs.dom.{MouseEvent, console}

import scala.collection.mutable
import scalatags.JsDom.all._


final case class Layer(name: String) {
  val nodes = mutable.Set[GraphNode]()
  val links = mutable.Set[GraphLink]()

  type Methods = Map[String, Seq[String]]

  /**
    * Add a MethodNode to the graph
    *
    * @param source  the source of the link
    * @param methods all the reachable methods from the source, grouped by className
    */
  def addMethods(source: GraphNode, methods: Methods) = {

    /**
      * Find all subclasses of a given ClassNode
      *
      * @param root the node we want to find the subclasses of
      * @return a sequence of nodes that have root as parent
      */
    def subClasses(root: ClassNode) =
      D3Graph.getCallGraph.classes.filter(_.superClass.fold(false)(_ == root.encodedName))

    /**
      * Add a MethodNode to the graph given its encodedName,
      * This function must not only look in the given class, but also in all its children.
      *
      * @param method the name of the method
      * @param root   the class in which we should look the method in
      */
    def addMethodNode(root: ClassNode, method: String) = {
      root.methods.find(_.encodedName == method) match {
        case Some(methNode) =>
          addNodeToGraph(root, methNode)
        case None =>
          for (cl <- subClasses(root)) cl.methods.find(_.encodedName == method).map(addNodeToGraph(cl, _))
      }
    }

    /**
      * Create a new GraphNode from a MethodNode, and add it to the graph with a link.
      * Link it with the given source
      */
    def addNodeToGraph(cn: ClassNode, mn: MethodNode, group: Int = 2) = {
      val newNode = GraphNode(Decoder.decodeMethod(cn.encodedName, mn.encodedName), group, mn)
      nodes += newNode
      links += GraphLink(source, newNode)
    }

    /**
      * Add a link from source to methodName in node to the graph
      *
      * @param cn
      * @param method
      */
    def addLinkToGraph(cn: ClassNode, method: String) = {
      nodes.find(_.data.encodedName == method) match {
        // Find the node in the graph
        case Some(graphNode) => links += GraphLink(source, graphNode)
        case None => addMethodNode(cn, method)
      }
      D3Graph.update()
    }

    for ((c, ms) <- methods) {
      D3Graph.getCallGraph.classes.find(_.encodedName == c) match {
        case Some(classNode) => ms.foreach(addLinkToGraph(classNode, _))
        case _ => console.log("no class found " + c)
      }
    }
  }
}

object Layers {
  private var selected = 0
  private val layers = mutable.ArrayBuffer[Layer]()

  def addLayer(name: String = "layer" + (layers.size + 1)): Layer = {
    layers += Layer(name)
    last()
  }

  def current(): Layer = layers.size match {
    case 0 => addLayer()
    case _ => layers(selected)
  }

  private def next(): Layer = {
    if (selected < layers.size - 1)
      selected += 1
    current()
  }

  private def previous(): Layer = {
    if (selected > 0) selected -= 1
    current()
  }

  private def setCurrent(i: Int): Unit = {
    if (i != selected && i >= 0 && i < layers.size) {
      selected = i
    }
  }

  private def last(): Layer = {
    setCurrent(layers.size - 1)
    current()
  }

  def toHTMLList = ul(
    for ((layer, index) <- layers.zipWithIndex; active = if (index == selected) "active" else "inactive") yield
      li()(a(href := "", onclick := changeLayer(index), `class` := active)(layer.name))
  ).render

  private def changeLayer(index: Int) = (e: MouseEvent) => {
    e.preventDefault()
    setCurrent(index)
    D3Graph.update()
    Visualization.showLayers()
  }

}
