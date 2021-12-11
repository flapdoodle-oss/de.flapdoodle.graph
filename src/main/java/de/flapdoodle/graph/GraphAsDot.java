/**
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

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Default;
import org.jgrapht.Graph;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
public abstract class GraphAsDot<T> {

	@Parameter
	public abstract Function<T, String> nodeAsId();

	@Default
	public Function<T, String> nodeAsLabel() {
		return nodeAsId();
	}

	@Default
	public BiFunction<T, T, Map<String, String>> edgeAttributes() {
		return (a, b) -> Collections.emptyMap();
	}

	@Default
	public Function<T, Map<String, String>> nodeAttributes() {
		return (a) -> Collections.emptyMap();
	}

	@Default
	public Function<T, Optional<SubGraph<T>>> subGraph() {
		return (a) -> Optional.empty();
	}

	@Default
	public String label() {
		return "graph";
	}

	@Auxiliary
	public <E> String asDot(Graph<T, E> graph) {
		StringBuilder sb = new StringBuilder();

		sb.append("digraph \"").append(label()).append("\" {\n")
			.append("	rankdir=LR;\n")
			.append("\n");

		AtomicInteger clusterCounter = new AtomicInteger();

		render(1, graph, sb, clusterCounter);

		sb.append("}\n");
		return sb.toString();
	}

	// TODO put this stuff into context
	private <E> void render(int level, Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		renderNodes(level, graph, sb, clusterCounter);
		sb.append("\n");

		List<Vertex2SubGraph<T>> subGraphs = graph.vertexSet().stream()
			.flatMap(v -> subGraph().apply(v)
				.map(Stream::of)
				.orElse(Stream.empty())
				.map(sub -> new Vertex2SubGraph<>(v, sub)))
			.collect(Collectors.toList());

		List<Vertext2VertexInSubGraph<T>> outerVertexToInnerVertexList = subGraphs.stream()
			.flatMap(sub -> sub.subGraph.connections().entrySet().stream()
				.map(entry -> new Vertext2VertexInSubGraph<>(
					entry.getKey(), new VertexInSubGraph<>(sub.vertext, entry.getValue()))
				)
			)
			.collect(Collectors.toList());

		Map<T, VertexInSubGraph<T>> outerVertexToInnerVertexMap = outerVertexToInnerVertexList.stream()
			.collect(Collectors.toMap(v -> v.vertex, v -> v.vertexInSubGraph));

		renderEdges(level, graph, outerVertexToInnerVertexMap, sb);
	}

	private <E> void renderNodes(int level, Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		graph.vertexSet().forEach(v -> {
			Map<String, String> nodeAttributes = nodeAttributes().apply(v);

			Optional<SubGraph<T>> subGraph = subGraph().apply(v);
			if (subGraph.isPresent()) {
				sb.append(indent(level)).append("subgraph cluster_" + clusterCounter.getAndIncrement() + " {\n");
				if (!nodeAttributes.isEmpty()) {
					sb.append(indent(level + 1)).append("node ").append(asNodeAttributes(nodeAttributes)).append(";\n");
				}

				render(level + 1, subGraph.get().graph(), sb, clusterCounter);
				sb.append(indent(level)).append("}\n");
			} else {
				sb.append(indent(level)).append(quote(nodeAsId().apply(v))).append(asNodeAttributes(nodeAttributes)).append(";\n");
			}
		});
	}

	private <E> void renderEdges(int level, Graph<T, E> graph, Map<T, VertexInSubGraph<T>> outerVertexToInnerVertexMap, StringBuilder sb) {
		graph.edgeSet().forEach((edge) -> {
			T a = graph.getEdgeSource(edge);
			T b = graph.getEdgeTarget(edge);

			VertexInSubGraph<T> innerA = outerVertexToInnerVertexMap.get(a);
			VertexInSubGraph<T> innerB = outerVertexToInnerVertexMap.get(b);

			if (!subGraph().apply(a).isPresent() && !subGraph().apply(b).isPresent()) {
				renderConnection(level, a, b, sb);
			} else {
				if (innerA != null) {
					renderConnection(level, nodeAsId().apply(a), nodeAsId().apply(innerA.vertex), edgeAttributes().apply(a, innerA.vertex),  sb);
				}
				if (innerB != null) {
					renderConnection(level, nodeAsId().apply(innerB.vertex), nodeAsId().apply(b), edgeAttributes().apply(innerB.vertex, b), sb);
				}
			}
		});
	}

	private void renderConnection(int level, T a, T b, StringBuilder sb) {
		renderConnection(level, nodeAsId().apply(a), nodeAsId().apply(b), edgeAttributes().apply(a, b), sb);
	}

	private static void renderConnection(int level, String a, String b, Map<String, String> edgeAttributes, StringBuilder sb) {
		sb.append(indent(level));
		sb.append(quote(a))
			.append(" -> ")
			.append(quote(b))
			.append(asNodeAttributes(edgeAttributes)).append(";\n");
	}

	private static String indent(int level) {
		return String.join("", Collections.nCopies(level, "\t"));
	}

	private static String asNodeAttributes(Map<String, String> map) {
		return map.isEmpty()
			? ""
			: "[ " + map.entrySet().stream().map(e -> e.getKey() + "=" + quote(e.getValue())).collect(Collectors.joining(", ")) + " ]";
	}

	private static String quote(String src) {
		return "\"" + src + "\"";
	}

	@Value.Immutable
	public interface SubGraph<T> {
		@Parameter
		Graph<T, ?> graph();

		Map<T, T> connections();

		static <T> ImmutableSubGraph.Builder<T> of(Graph<T, ?> graph) {
			return ImmutableSubGraph.builder(graph);
		}
	}

	private static final class Vertex2SubGraph<T> {
		private final T vertext;
		private final SubGraph<T> subGraph;

		public Vertex2SubGraph(T vertext, SubGraph<T> subGraph) {
			this.vertext = vertext;
			this.subGraph = subGraph;
		}
	}

	private static final class Vertext2VertexInSubGraph<T> {
		private final T vertex;
		private final VertexInSubGraph<T> vertexInSubGraph;

		public Vertext2VertexInSubGraph(T vertex, VertexInSubGraph<T> vertexInSubGraph) {
			this.vertex = vertex;
			this.vertexInSubGraph = vertexInSubGraph;
		}
	}

	private static final class VertexInSubGraph<T> {
		private final T parent;
		private final T vertex;

		public VertexInSubGraph(T parent, T vertex) {
			this.parent = parent;
			this.vertex = vertex;
		}
	}

	public static <T> ImmutableGraphAsDot.Builder<T> builder(Function<T, String> nodeAsId) {
		return ImmutableGraphAsDot.builder(nodeAsId);
	}
}
