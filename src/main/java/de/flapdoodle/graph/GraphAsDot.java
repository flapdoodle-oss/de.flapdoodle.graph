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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Default;
import org.jgrapht.Graph;

@Value.Immutable
public abstract class GraphAsDot<T> {

	@Parameter public abstract Function<T, String> nodeAsString();
	
	@Default
	public BiFunction<T, T, Map<String, String>> edgeAttributes() {
		return (a,b) -> Collections.emptyMap();
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
		StringBuilder sb=new StringBuilder();

		sb.append("digraph \"").append(label()).append("\" {\n")
			.append("	rankdir=LR;\n")
			.append("\n");

		AtomicInteger clusterCounter=new AtomicInteger();

		render(1, graph, sb, clusterCounter);

		sb.append("}\n");
		return sb.toString();
	}

	private <E> void render(int level, Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		renderNodes(level, graph, sb, clusterCounter);
		sb.append("\n");

		List<SubGraph<T>> subGraphs = graph.vertexSet().stream()
			.flatMap(v -> subGraph().apply(v).map(sub -> Stream.of(sub)).orElse(Stream.empty()))
			.collect(Collectors.toList());

		Map<T, T> outerVertexToInnerVertexMap = subGraphs.stream()
			.flatMap(sub -> sub.connections().entrySet().stream())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		renderEdges(level, graph, outerVertexToInnerVertexMap, sb);
	}

	private <E> void renderNodes(int level, Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		graph.vertexSet().forEach(v -> {
			Optional<SubGraph<T>> subGraph = subGraph().apply(v);
			if (subGraph.isPresent()) {
				    sb.append(indent(level)).append("subgraph cluster_"+ clusterCounter.getAndIncrement()+" {\n");
						sb.append(indent(level+1)).append("label = ").append(quote(nodeAsString().apply(v))).append(";\n");
						render(level + 1, subGraph.get().graph(), sb, clusterCounter);
						sb.append(indent(level)).append("}\n");
			} else {
				sb.append(indent(level)).append(quote(nodeAsString().apply(v))).append(asNodeAttributes(nodeAttributes().apply(v))).append(";\n");
			}
		});
	}

	private <E> void renderEdges(int level, Graph<T, E> graph, Map<T, T> outerVertexToInnerVertexMap, StringBuilder sb) {
		graph.edgeSet().forEach((edge) -> {
			T a = graph.getEdgeSource(edge);
			T b = graph.getEdgeTarget(edge);

			T innerA = outerVertexToInnerVertexMap.get(a);
			T innerB = outerVertexToInnerVertexMap.get(b);

			if (!subGraph().apply(a).isPresent() && !subGraph().apply(b).isPresent()) {
				sb.append(indent(level));
				renderConnection(level, a, b, sb);
			} else {
				if (innerA!=null) {
					sb.append(indent(level));
					renderConnection(level, a, innerA, sb);
				}
				if (innerB!=null) {
					sb.append(indent(level));
					renderConnection(level, innerB, b, sb);
				}
			}
		});
	}
	private void renderConnection(int level, T a, T b, StringBuilder sb) {
		sb.append(quote(nodeAsString().apply(a)))
			.append(" -> ")
			.append(quote(nodeAsString().apply(b)))
			.append(asNodeAttributes(edgeAttributes().apply(a, b))).append(";\n");
	}

	protected String indent(int level) {
		return String.join("", Collections.nCopies(level, "\t"));
	}

	@Auxiliary
	public String asNodeAttributes(Map<String, String> map) {
		return map.isEmpty() 
				? "" 
				: "[ "+map.entrySet().stream().map(e -> e.getKey()+"="+quote(e.getValue())).collect(Collectors.joining(", ")) +" ]";
	}

	@Auxiliary
	public String quote(String src) {
		return "\""+src+"\"";
	}

	@Value.Immutable
	public interface SubGraph<T> {
		@Parameter
		Graph<T, ?> graph();

		Map<T, T> connections();

		static <T> ImmutableSubGraph.Builder<T> of(Graph<T,?> graph) {
			return ImmutableSubGraph.builder(graph);
		}
	}

	private static class Pair<A,B> {
		private final A a;
		private final B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}

		public A a() {
			return a;
		}

		public B b() {
			return b;
		}
	}

	public static <T> ImmutableGraphAsDot.Builder<T> builder(Function<T, String> nodeAsString) {
		return ImmutableGraphAsDot.builder(nodeAsString);
	}
}
