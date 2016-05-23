import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Example extends JSApp {
  def main(): Unit = {
    foo(42)
    println("The world's a better place...")
    Set(1,2,3)
  }

  @JSExport
  def foo(i: Int): Unit =
    println("Hello world!" +  i)
}
