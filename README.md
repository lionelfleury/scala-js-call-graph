# Call graph visualisation tool for Scala.js
Add the following to your project definition files:

`<root>/project/plugins.sbt`
```scala
addSbtPlugin("com.github.lionelfleury" % "sbt-scalajs-callgraph" % "0.1.0")
```
`<root>/build.sbt`
```scala
enablePlugins(CallGraphPlugin)
```
## Usage
Run the new added SBT command:
```scala
sbt callgraph
```
You get `graph.json` and `index.html` in your target directory.

Open the html file and start exploring your Scala.js project's call graph...
## Snapshot
If you want the development stage version, change to the following:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.github.lionelfleury" % "sbt-scalajs-callgraph" % "0.1.1-SNAPSHOT")
```
