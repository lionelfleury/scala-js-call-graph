import org.scalajs.jsenv.selenium.Firefox

val commonSettings = Seq(
  organization := "com.github.lionelfleury",
  version := "0.1.3-SNAPSHOT",
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings", "-encoding", "utf-8"),
  ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))
)

val testSettings = Seq(
  testOptions += Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-v", "-a"),
  jsDependencies ++= Seq(
    RuntimeDOM % "test",
    "org.webjars" % "jquery" % "1.10.2" / "jquery.js"),
  jsEnv in Test := new org.scalajs.jsenv.selenium.SeleniumJSEnv(Firefox)
)

val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
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
)

lazy val root = (project in file(".")).
  enablePlugins(CrossPerProjectPlugin).
  aggregate(utilsJS, utilsJVM, `sbt-scalajs-callgraph`).
  settings(commonSettings: _*).
  settings(publishSettings: _*).
  settings(
    aggregate in update := false
  )


lazy val `sbt-scalajs-callgraph` = (project in file("sbt-plugin")).
  settings(commonSettings: _*).
  settings(
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    managedResources in Compile ++= Seq(
      (fullOptJS in Compile in callgraph).value.data,
      (packageScalaJSLauncher in Compile in callgraph).value.data,
      (packageJSDependencies in Compile in callgraph).value
    ),
    unmanagedResources in Compile += baseDirectory.value.getParentFile / "callgraph-style.css",
    fastOptJS := (fastOptJS in Compile in callgraph).value
  ).dependsOn(utilsJVM)

lazy val callgraph = (project in file("visualisation")).
  enablePlugins(ScalaJSPlugin, ScalaJSJUnitPlugin).
  settings(commonSettings: _*).
  settings(testSettings: _*).
  settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.singlespaced" %%% "scalajs-d3" % "0.3.3",
      "org.scala-js" %%% "scalajs-ir" % scalaJSVersion,
      "com.lihaoyi" %%% "scalatags" % "0.5.5"),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    publish :=(),
    publishLocal :=()
  ).dependsOn(utilsJS)

lazy val `callgraph-utils` = (crossProject in file("utils")).
  settings(commonSettings: _*).
  settings(
    crossScalaVersions := Seq("2.10.6", "2.11.8"),
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.0"
  )

lazy val utilsJS = `callgraph-utils`.js
lazy val utilsJVM = `callgraph-utils`.jvm
