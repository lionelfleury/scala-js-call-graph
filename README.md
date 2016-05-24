# Call graph visualisation tool for Scala.js

`project/plugins.sbt`
```scala
addSbtPlugin("com.github.lionelfleury" % "sbt-scalajs-callgraph" % "0.1.0")
```
# Usage

```scala
sbt callgraph
```
You get `graph.json` and `index.html` in your target directory.

Open the html file and start exploring your Scala.js project's call graph...

