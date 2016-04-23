import org.scalajs.jsenv.selenium.{CustomFileMaterializer, Firefox}

ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "ch.epfl",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= Seq(
      "-deprecation", "-feature", "-Xfatal-warnings", "-encoding", "utf-8"),
  homepage := Some(url("https://github.com/lionelfleury/scala-js-call-graph")),
  licenses += ("MIT", url("http://opensource.org/licenses/mit-license.php")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/lionelfleury/scala-js-call-graph"),
      "scm:git:git@github.com:lionelfleury/scala-js-call-graph",
      Some("scm:git@github.com:lionelfleury/scala-js-call-graph.git"))),
  publishMavenStyle := true
)

lazy val `sbt-scalajs-callgraph-utils` =
  (crossProject in file("sbt-scalajs-callgraph-utils")).
    settings(commonSettings: _*).
    settings(
      libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.8",
      crossScalaVersions := Seq("2.10.6", "2.11.8")
    )

lazy val utilsJS = `sbt-scalajs-callgraph-utils`.js
lazy val utilsJVM = `sbt-scalajs-callgraph-utils`.jvm

lazy val `sbt-scalajs-callgraph` = (project in file("sbt-scalajs-callgraph")).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalaJSVersion
  ).dependsOn(utilsJVM)

val testSettings: Seq[Setting[_]] = commonSettings ++ Seq(
  testOptions +=
    Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-v", "-a"),
  jsDependencies ++= Seq(
    RuntimeDOM % "test",
    "org.webjars" % "jquery" % "1.10.2" / "jquery.js"
  )
)

lazy val `scalajs-callgraph` = (project in file(".")).
  enablePlugins(ScalaJSPlugin).
  enablePlugins(ScalaJSJUnitPlugin).
  enablePlugins(CallGraphPlugin).
  settings(commonSettings: _*).
  settings(testSettings).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.singlespaced" %%% "scalajs-d3" % "0.3.1",
      "org.scala-js" %% "scalajs-env-selenium" % "0.1.2",
      "com.lihaoyi" %%% "upickle" % "0.3.8",
      "com.lihaoyi" %%% "scalatags" % "0.5.4"),
    jsDependencies ++= Seq(
      "org.webjars" % "d3js" % "3.5.12" / "3.5.12/d3.js",
      RuntimeDOM),
    jsEnv in Test := new org.scalajs.jsenv.selenium.SeleniumJSEnv(Firefox).
      withMaterializer(new CustomFileMaterializer("index.html",   "file://" + file(".").getAbsolutePath + "/src/")),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    publish := {},
    publishLocal := {}).
  dependsOn(utilsJVM, utilsJS)
