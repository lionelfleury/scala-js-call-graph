import org.scalajs.jsenv.selenium.Firefox

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.github.lionelfleury",
  version := "0.1.3-SNAPSHOT",
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings", "-encoding", "utf-8")
)

val testSettings = Seq(
  testOptions += Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-v", "-a"),
  jsDependencies ++= Seq(
    RuntimeDOM % "test",
    "org.webjars" % "jquery" % "1.10.2" / "jquery.js")
)

lazy val `scalajs-callgraph` = (project in file(".")).
  enablePlugins(ScalaJSPlugin).
  enablePlugins(ScalaJSJUnitPlugin).
  enablePlugins(CallGraphPlugin).
  enablePlugins(CrossPerProjectPlugin).
  settings(commonSettings: _*).
  settings(testSettings: _*).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.singlespaced" %%% "scalajs-d3" % "0.3.3",
      "com.lihaoyi" %%% "upickle" % "0.4.0",
      "org.scala-js" %%% "scalajs-ir" % scalaJSVersion,
      "com.lihaoyi" %%% "scalatags" % "0.5.5"),
    jsDependencies ++= Seq(
      "org.webjars" % "d3js" % "3.5.16" / "3.5.16/d3.js",
      RuntimeDOM),
    jsEnv in Test := new org.scalajs.jsenv.selenium.SeleniumJSEnv(Firefox),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    publish :=(),
    publishLocal :=()).
  dependsOn(utilsJVM, utilsJS).
  aggregate(utilsJVM, utilsJS, `sbt-scalajs-callgraph`)

lazy val `sbt-scalajs-callgraph` = (project in file("sbt-scalajs-callgraph")).
  settings(commonSettings: _*).
  settings(
    crossScalaVersions := Seq("2.10.6"),
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalaJSVersion).
  dependsOn(utilsJVM)

lazy val `sbt-scalajs-callgraph-utils` = (crossProject in file("sbt-scalajs-callgraph-utils")).
  settings(commonSettings: _*).
  settings(
    crossScalaVersions := Seq("2.10.6", "2.11.8"),
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.0"
  )

lazy val utilsJS = `sbt-scalajs-callgraph-utils`.js
lazy val utilsJVM = `sbt-scalajs-callgraph-utils`.jvm

ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra in Global := {
  <url>https://github.com/lionelfleury/scala-js-call-graph</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/lionelfleury/scala-js-call-graph</connection>
      <developerConnection>scm:git:git@github.com:lionelfleury/scala-js-call-graph</developerConnection>
      <url>github.com/lionelfleury/scala-js-call-graph.git</url>
    </scm>
    <developers>
      <developer>
        <id>lionelfleury</id>
        <name>Lionel Fleury</name>
        <url>https://github.com/lionelfleury/</url>
      </developer>
      <developer>
        <id>ex0ns</id>
        <name>Guillaume Tournigand</name>
        <url>https://github.com/ex0ns/</url>
      </developer>
    </developers>
}
