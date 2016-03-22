unmanagedSourceDirectories in Compile ++= Seq(
  baseDirectory.value.getParentFile / "sbt-scalajs-callgraph/src/main/scala",
  baseDirectory.value.getParentFile / "sbt-scalajs-callgraph-utils/shared/src/main/scala")

libraryDependencies += "com.lihaoyi" %% "upickle" % "0.3.8"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")
