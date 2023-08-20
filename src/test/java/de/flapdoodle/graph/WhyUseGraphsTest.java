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

import de.flapdoodle.graph.doc.Base;
import de.flapdoodle.graph.doc.Embedded;
import de.flapdoodle.graph.doc.Named;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.assertj.core.api.*;
import org.assertj.core.util.Sets;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WhyUseGraphsTest {
	@RegisterExtension
	public static Recording recording = Recorder.with("WhyUseGraphs.md", TabSize.spaces(2));

	@Test
	public void simpleGraph() {
		recording.begin("vertex");
		Named a = Named.of("A");
		Named b = Named.of("B");
		Named c = Named.of("C");

		Named embeddedA = Named.of("a");
		Named embeddedB = Named.of("b");
		recording.end();

		recording.begin("embedded");
		Embedded embedded = Embedded.builder()
			.name("embedded")
			.graph(GraphBuilder.<Base>withDirectedGraph()
				.addEdgeChain(embeddedA, embeddedB)
				.build())
			.putConnections(b, embeddedA)
			.putConnections(c, embeddedB)
			.build();
		recording.end();

		recording.begin("graph");
		DefaultDirectedGraph<Base, DefaultEdge> graph = GraphBuilder.<Base>withDirectedGraph()
			.addEdgeChain(a, b, embedded, c)
			.build();
		recording.end();

		recording.begin("dotFile");
		String dotFile = GraphAsDot.builder(Base::name)
			.subGraphIdSeparator("__")
			.label("label")
			.nodeAsLabel(vertex -> "label " + vertex.name())
			.nodeAttributes(vertex -> asMap("shape", "rectangle"))
			.sortedBy(Base::name)
			.subGraph(vertex -> vertex instanceof Embedded
				? Optional.of(((Embedded) vertex).subGraph())
				: Optional.empty())
			.build()
			.asDot(graph);
		recording.end();
		recording.output("graph.dot", dotFile);
		recording.file("graph.dot.svg", "WhyUseGraphs.svg", GraphvizAdapter.asSvg(dotFile));
	}

	@Test
	public void sampleGraph() {
		DefaultDirectedGraph<String, DefaultEdge> graph = sample();
		recording.file("graph.dot.svg", "WhyUseGraphs-Sample.svg", asSvg(graph));
	}

	@Test
	public void filter() {
		recording.begin();
		DefaultDirectedGraph<String, DefaultEdge> graph = sample();

		DefaultDirectedGraph<String, DefaultEdge> filtered = Graphs.filter(graph, it -> !it.equals("2"));
		recording.end();
		recording.file("graph.dot.svg", "WhyUseGraphs-Filter.svg", asSvg(filtered));
	}

	@Test
	public void shaveTheTree() {
		recording.begin();
		DefaultDirectedGraph<String, DefaultEdge> graph = sample();
		List<VerticesAndEdges<String, DefaultEdge>> leaves = Graphs.leavesOf(graph);

		assertThat(leaves).hasSize(5);

		VerticesAndEdgesAssert.assertThat(leaves.get(0))
			.containsVertices("d", "Y")
			.containsEdges("c->d", "2->d", "X->Y")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(leaves.get(1))
			.containsVertices("c", "2")
			.containsEdges("b->c", "1->2", "b->2")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(leaves.get(2))
			.containsVertices("b")
			.containsEdges("a->b", "0->b")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(leaves.get(3))
			.containsVertices("0")
			.containsEdges()
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(leaves.get(4))
			.containsVertices("a", "1", "X")
			.containsEdges()
			.hasLoops(1)
			.containsLoop("1->X", "X->a","a->1");
		recording.end();
		recording.file("graph.dot.svg", "WhyUseGraphs-ShaveTheTree.svg", asSvg(graph, node -> {
			switch (node) {
				case "d":
				case "Y":
					return asMap("color","red");
				case "c":
				case "2":
					return asMap("color","orange");
				case "b":
					return asMap("color","gold");
				case "0":
					return asMap("color","green");
				case "a":
				case "1":
				case "X":
					return asMap("color","blue");
			}
			return Collections.emptyMap();
		}));
	}

	@Test
	public void climbTheTree() {
		recording.begin();
		DefaultDirectedGraph<String, DefaultEdge> graph = sample();
		List<VerticesAndEdges<String, DefaultEdge>> roots = Graphs.rootsOf(graph);

		assertThat(roots).hasSize(5);

		VerticesAndEdgesAssert.assertThat(roots.get(0))
			.containsVertices("0")
			.containsEdges("0->b")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(roots.get(1))
			.containsVertices("a", "1", "X")
			.containsEdges()
			.hasLoops(1)
			.containsLoop("1->X", "X->a","a->1");

		VerticesAndEdgesAssert.assertThat(roots.get(2))
			.containsVertices("b", "Y")
			.containsEdges("b->c", "b->2")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(roots.get(3))
			.containsVertices("c", "2")
			.containsEdges("c->d", "2->d")
			.hasLoops(0);

		VerticesAndEdgesAssert.assertThat(roots.get(4))
			.containsVertices("d")
			.containsEdges()
			.hasLoops(0);

		recording.end();
		recording.file("graph.dot.svg", "WhyUseGraphs-ClimbTheTree.svg", asSvg(graph, node -> {
			switch (node) {
				case "0":
					return asMap("color","red");
				case "a":
				case "1":
				case "X":
					return asMap("color","orange");
				case "b":
				case "Y":
					return asMap("color","gold");
				case "c":
				case "2":
					return asMap("color","green");
				case "d":
					return asMap("color","blue");
			}
			return Collections.emptyMap();
		}));
	}

	private Pair<String, String> edge(String start, String end) {
		return new Pair<>(start, end);
	}

	private static DefaultDirectedGraph<String, DefaultEdge> sample() {
		return GraphBuilder.<String>withDirectedGraph()
			.addEdgeChain("a","b","c","d")
			.addEdgeChain("a", "1", "2", "d")
			.addEdgeChain("1", "X", "Y")
			.addEdgeChain("0", "b", "2")
			.addEdgeChain("X", "a")
			.build();
	}

	private static byte[] asSvg(DefaultDirectedGraph<String, DefaultEdge> graph) {
		return asSvg(graph, node -> Collections.emptyMap());
	}

	private static byte[] asSvg(DefaultDirectedGraph<String, DefaultEdge> graph, Function<String, Map<String, String>> nodeAttributes) {
		return GraphvizAdapter.asSvg(asDot(graph, nodeAttributes));
	}

	private static String asDot(DefaultDirectedGraph<String, DefaultEdge> graph) {
		return asDot(graph, node -> Collections.emptyMap());
	}

	private static String asDot(DefaultDirectedGraph<String, DefaultEdge> graph, Function<String, Map<String, String>> nodeAttributes) {
		return GraphAsDot.builder(Function.identity())
			.nodeAttributes(nodeAttributes)
			.build()
			.asDot(graph);
	}

	private Map<String, String> asMap(String key, String value) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put(key,value);
		return map;
	}

	static class Pair<A, B> {
		private final A a;
		private final B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Pair<?, ?> pair = (Pair<?, ?>) o;
			return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
		}

		@Override
		public int hashCode() {
			return Objects.hash(a, b);
		}

		@Override
		public String toString() {
			return a+"->"+b;
		}
	}

	static class VerticesAndEdgesAssert<V, E> extends AbstractObjectAssert<VerticesAndEdgesAssert<V, E>, VerticesAndEdges<V, E>> {

		public VerticesAndEdgesAssert(VerticesAndEdges<V, E> verticesAndEdges) {
			super(verticesAndEdges, VerticesAndEdgesAssert.class);
		}

		@SafeVarargs
		public final VerticesAndEdgesAssert<V, E> containsVertices(V... vertices) {
			extracting(VerticesAndEdges::vertices, InstanceOfAssertFactories.<V>collection((Class<V>) vertices[0].getClass()))
				.containsExactlyInAnyOrder(vertices);

			return myself;
		}

		@SafeVarargs
		public final VerticesAndEdgesAssert<V, E> containsEdges(String ... edges) {
			extracting(VerticesAndEdges::edges, InstanceOfAssertFactories.collection(Edge.class))
				.map(edge -> edge.start()+"->"+edge.end())
				.containsExactlyInAnyOrder(edges);

			return myself;
		}

		public VerticesAndEdgesAssert<V, E> containsLoop(String ... loopParts) {
			extracting(VerticesAndEdges::loops, InstanceOfAssertFactories.collection((Class<Loop<V, E>>) (Class) Loop.class))
				.map(loop -> loop.edges().stream().map(e -> e.start()+"->"+e.end()).collect(Collectors.toSet()))
				.contains(Sets.newLinkedHashSet(loopParts));

			return myself;
		}

		public VerticesAndEdgesAssert<V, E> hasLoops(int loopCount) {
			extracting(VerticesAndEdges::loops, InstanceOfAssertFactories.collection((Class<Loop<V, E>>) (Class) Loop.class))
				.hasSize(loopCount);

			return myself;
		}

		private static <V, E> VerticesAndEdgesAssert<V, E> assertThat(VerticesAndEdges<V, E> instance) {
			return new VerticesAndEdgesAssert<>(instance);
		}

		public void foo() {

		}
	}
}
