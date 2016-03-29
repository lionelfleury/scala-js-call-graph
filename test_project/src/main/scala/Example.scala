import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Example extends JSApp {
  def main(): Unit = {
    foo()
  }

  @JSExport
  def foo(): Unit =
    println("Hello world!")
}
