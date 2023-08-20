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
${simpleGraph.graph.dot}
```

which looks like this:

![Simple Graph](${simpleGraph.graph.dot.svg})
                         
## Walk the Graph

Given a sample graph

![Sample](${sampleGraph.graph.dot.svg})
                                       
you can filter the graph:

```java
${filter}
```

![Filtered](${filter.graph.dot.svg})

you can traverse the graph starting with leaves:

```java
${shaveTheTree}
```

![Shave The Tree](${shaveTheTree.graph.dot.svg})

or the roots:

```java
${climbTheTree}
```

![Climb The Tree](${climbTheTree.graph.dot.svg})

