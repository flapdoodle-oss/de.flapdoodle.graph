/*
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
