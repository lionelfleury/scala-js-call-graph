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


It's also possible to generate the callgraph for your tests simply use:
```scala
sbt test:callgraph
```

You get `callgraph.html` (or `callgraph-test.html`) in your target directory.
Open the html file and start exploring your Scala.js project's call graph...
## Snapshot
If you want the development stage version, change to the following:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.github.lionelfleury" % "sbt-scalajs-callgraph" % "0.1.1-SNAPSHOT")
```

## Development
If you're interested in contributing to this project, simply fork the repo and open a new pull request when you're ready.
If you're working on the ScalaJS part, you might want to use the command:
```scala
sbt callgraph-dev
```
to test your modifications. This command generates a `callgraph-dev.html` using the local version of the javascript files. 

By default, the `callgraph.html` is using javascript files hosted on the release branch of the repo.

