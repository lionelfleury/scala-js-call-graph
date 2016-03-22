crossScalaVersions := Seq("2.10.6", "2.11.7")

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "ch.epfl",

  version := "0.1.0-SNAPSHOT",

  scalacOptions ++= Seq(
      "-deprecation", "-feature", "-Xfatal-warnings",
      "-encoding", "utf-8"),

  homepage := Some(url("https://github.com/lionelfleury/scala-js-call-graph")),
  licenses += ("MIT", url("http://opensource.org/licenses/mit-license.php")),

  scmInfo := Some(ScmInfo(
      url("https://github.com/lionelfleury/scala-js-call-graph"),
      "scm:git:git@github.com:sjrd/scalajs-reflect.git",
      Some("scm:git@github.com:lionelfleury/scala-js-call-graph.git")))
)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  "com.lihaoyi" %%% "upickle" % "0.3.8",
  "org.scala-js" %% "scalajs-tools" % scalaJSVersion)

lazy val `sbt-scalajs-callgraph` = project.in(file("sbt-scalajs-callgraph")).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8"),
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-tools" % scalaJSVersion
    )
  ).dependsOn(utilsJVM)

lazy val `sbt-scalajs-callgraph-utils` = crossProject.in(file("sbt-scalajs-callgraph-utils")).
  settings(commonSettings: _*).
  settings(libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.8")

lazy val utilsJS = `sbt-scalajs-callgraph-utils`.js

lazy val utilsJVM = `sbt-scalajs-callgraph-utils`.jvm

lazy val `scalajs-callgraph` = project.in(file(".")).
  enablePlugins(ScalaJSPlugin).
  enablePlugins(CallGraphPlugin).
  settings(commonSettings: _*).
  settings(
    scalaVersion := "2.11.7",
    libraryDependencies += "org.singlespaced" %%% "scalajs-d3" % "0.3.0",
    jsDependencies ++= Seq(
      "org.webjars" % "d3js" % "3.5.12" / "3.5.12/d3.js",
      RuntimeDOM
    ),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false
  )
