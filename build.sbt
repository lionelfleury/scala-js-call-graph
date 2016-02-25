lazy val root = (project in file(".")).
  enablePlugins(ScalaJSPlugin)

name := "Scala.js Call Graph"

licenses += ("MIT", url("http://opensource.org/licenses/mit-license.php"))

scalaVersion := "2.11.7"
