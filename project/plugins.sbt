unmanagedSourceDirectories in Compile ++= {
  val root = baseDirectory.value.getParentFile
  Seq(
    root / "sbt-scalajs-callgraph/src/main/scala",
    root / "sbt-scalajs-callgraph-utils/shared/src/main/scala"
  )
}

libraryDependencies += "com.lihaoyi" %% "upickle" % "0.3.8"

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "0.1.2"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
