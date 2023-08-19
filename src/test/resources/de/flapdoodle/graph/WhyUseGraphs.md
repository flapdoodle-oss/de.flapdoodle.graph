#Why use Graphs?

In this simple example we can declare a graph with some custom vertex classes:

```java
${simpleGraph.vertex}
```

It is possible to create subgraphs:

```java
${simpleGraph.embedded}
```

Create a graph with all connections:

```java
${simpleGraph.graph}
```

Create a dot file from this graph:

```java
${simpleGraph.dotFile}
```

```dot
${simpleGraph.copy-file.dot}
```

which looks like this:

![Example-Dot](${simpleGraph.copy-file.dot.svg})
