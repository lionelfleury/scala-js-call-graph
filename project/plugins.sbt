unmanagedSourceDirectories in Compile ++= Seq(
  baseDirectory.value.getParentFile / "sbt-scalajs-callgraph/src/main/scala",
  baseDirectory.value.getParentFile / "sbt-scalajs-callgraph-utils/shared/src/main/scala")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")


