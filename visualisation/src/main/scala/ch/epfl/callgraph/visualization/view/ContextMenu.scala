package ch.epfl.callgraph.visualization.view

import org.scalajs.dom.raw.Event

import scalatags.JsDom.all._

object ContextMenu {

  val newLayerLink = li(`class` := "context-menu__item")(
    a(`class` := "context-menu__link")(
      i(`class` := "fa fa-eye")("Expand node to new layer")
    )
  ).render

  val expandLink = li(`class` := "context-menu__item")(
    a(`class` := "context-menu__link")(
      i(`class` := "fa fa-eye")("Expand node forward")
    )
  ).render

  val hideLink = li(`class` := "context-menu__item")(
    a(`class` := "context-menu__link")(
      i(`class` := "fa fa-eye")("Hide node")
    )
  ).render

  val nav = div(`class` := "context-menu")(
    ul(`class` := "context-menu__items")(
      expandLink,
      hideLink,
      newLayerLink
    )
  ).render

  def show(x: Double, y: Double, unit: String = "px") = {
    nav.setAttribute("class", "context-menu--active context-menu")
    nav.setAttribute("style", "left:" + x + unit + " ;top:" + y + unit)
  }

  def hide(): Unit = nav.setAttribute("class", "context-menu")

  /**
    * Context menu actions callback
    */
  def setNewLayerCallback(f: Event => Unit) = newLayerLink.onclick = f

  def setExpandCallback(f: Event => Unit) = expandLink.onclick = f

  def setHideCallback(f: Event => Unit) = hideLink.onclick = f

}
