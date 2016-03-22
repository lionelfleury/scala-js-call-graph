val commonSettings: Seq[Setting[_]] = Seq(
  organization := "ch.epfl",
  name := "Scala.js CallGraph",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= Seq(
      "-deprecation", "-feature", "-Xfatal-warnings",
      "-encoding", "utf-8"),
  homepage := Some(url("https://github.com/lionelfleury/scala-js-call-graph")),
  licenses += ("MIT", url("http://opensource.org/licenses/mit-license.php")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/lionelfleury/scala-js-call-graph"),
      "scm:git:git@github.com:lionelfleury/scala-js-call-graph",
      Some("scm:git@github.com:lionelfleury/scala-js-call-graph.git"))),
  ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))
)

lazy val `sbt-scalajs-callgraph` = (project in file("sbt-scalajs-callgraph")).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalaJSVersion
  ).dependsOn(utilsJVM)

lazy val `sbt-scalajs-callgraph-utils` = (crossProject in file("sbt-scalajs-callgraph-utils")).
  settings(commonSettings: _*).
  settings(libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.8")

lazy val utilsJS = `sbt-scalajs-callgraph-utils`.js
lazy val utilsJVM = `sbt-scalajs-callgraph-utils`.jvm

lazy val `scalajs-callgraph` = (project in file(".")).
  enablePlugins(ScalaJSPlugin).
  enablePlugins(CallGraphPlugin).
  settings(commonSettings: _*).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies += "org.singlespaced" %%% "scalajs-d3" % "0.3.1",
    jsDependencies ++= Seq(
      "org.webjars" % "d3js" % "3.5.12" / "3.5.12/d3.js",
      RuntimeDOM
    ),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false
  ).aggregate(utilsJS, `sbt-scalajs-callgraph`)
