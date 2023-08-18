package de.flapdoodle.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class GraphBuilder<V, E, G extends Graph<V, E>> {

	private final G graph;

	public GraphBuilder(G graph) {
		this.graph = graph;
	}

	public G build() {
		return graph;
	}

	public GraphBuilder<V, E, G> addVertex(V v) {
		graph.addVertex(v);
		return this;
	}

	public GraphBuilder<V, E, G> addEdge(V a, V b) {
		graph.addEdge(a, b);
		return this;
	}

	public GraphBuilder<V, E, G> addEdge(V a, V b, E edge) {
		graph.addEdge(a, b, edge);
		return this;
	}

	@SuppressWarnings("unchecked")
	public GraphBuilder<V, E, G> addVertices(V a, V b, V... other) {
		graph.addVertex(a);
		graph.addVertex(b);
		for (V o : other) {
			graph.addVertex(o);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public GraphBuilder<V, E, G> addEdgeChain(V a, V b, V... other) {
		addVertices(a, b, other);

		graph.addEdge(a, b);
		V last = b;
		for (V o : other) {
			graph.addEdge(last, o);
			last = o;
		}
		return this;
	}

	public static <V, E, G extends Graph<V, E>> GraphBuilder<V, E, G> of(G graph) {
		return new GraphBuilder<>(graph);
	}

	public static <V> GraphBuilder<V, DefaultEdge, DefaultDirectedGraph<V, DefaultEdge>> withDirectedGraph() {
		return new GraphBuilder<>(Graphs.Directed.newInstance());
	}

	public static <V, E> GraphBuilder<V, E, DefaultDirectedGraph<V, E>> withDirectedGraph(Class<E> edgeType) {
		return new GraphBuilder<>(Graphs.Directed.newInstance(edgeType));
	}
}
