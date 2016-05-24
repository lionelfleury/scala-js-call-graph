import org.scalajs.jsenv.selenium.{CustomFileMaterializer, Firefox}

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "com.github.lionelfleury",
  version := "0.1.1-SNAPSHOT",
  scalacOptions ++= Seq(
    "-deprecation", "-feature", "-Xfatal-warnings", "-encoding", "utf-8")
)

lazy val `sbt-scalajs-callgraph-utils` =
  (crossProject in file("sbt-scalajs-callgraph-utils")).
    settings(commonSettings: _*).
    settings(
      crossScalaVersions := Seq("2.10.6", "2.11.8"),
      libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.8"
    )

lazy val utilsJS = `sbt-scalajs-callgraph-utils`.js
lazy val utilsJVM = `sbt-scalajs-callgraph-utils`.jvm

lazy val `sbt-scalajs-callgraph` = (project in file("sbt-scalajs-callgraph")).
  settings(commonSettings: _*).
  settings(
    scalaVersion := "2.10.6",
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalaJSVersion
  ).dependsOn(utilsJVM).aggregate(utilsJVM, utilsJS)

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
    jsEnv in Test := new org.scalajs.jsenv.selenium.SeleniumJSEnv(Firefox),
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    publishLocal := {
      "sbt sbt-scalajs-callgraph/publishLocal" +
        " reload" +
        " +sbt-scalajs-callgraph-utilsJS/publishLocal" +
        " +sbt-scalajs-callgraph-utilsJVM/publishLocal" +
        " scalajs-callgraph/fastOptJS" !
    }).
  dependsOn(utilsJVM, utilsJS)

ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true))

publishMavenStyle := true

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

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false
