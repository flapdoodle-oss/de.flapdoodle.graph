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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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

		render(graph, sb, clusterCounter);

		sb.append("}\n");
		return sb.toString();
	}

	private <E> void render(Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		renderNodes(graph, sb, clusterCounter);
		sb.append("\n");
		renderEdges(graph, sb);
	}

	private <E> void renderNodes(Graph<T, E> graph, StringBuilder sb, AtomicInteger clusterCounter) {
		graph.vertexSet().forEach(v -> {
			Optional<SubGraph<T>> subGraph = subGraph().apply(v);
			if (subGraph.isPresent()) {
				    sb.append("subgraph cluster_"+ clusterCounter.getAndIncrement()+" {\n");
						render(subGraph.get().graph(), sb, clusterCounter);
						sb.append("}\n");
			} else {
				sb.append("\t").append(quote(nodeAsString().apply(v))).append(asNodeAttributes(nodeAttributes().apply(v))).append(";\n");
			}
		});
	}

	private <E> void renderEdges(Graph<T, E> graph, StringBuilder sb) {
		graph.edgeSet().forEach((edge) -> {
			T a = graph.getEdgeSource(edge);
			T b = graph.getEdgeTarget(edge);
			sb.append("\t")
				.append(quote(nodeAsString().apply(a)))
				.append(" -> ")
				.append(quote(nodeAsString().apply(b)))
				.append(asNodeAttributes(edgeAttributes().apply(a,b))).append(";\n");
		});
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

		static <T> ImmutableSubGraph.Builder<T> of(Graph<T,?> graph) {
			return ImmutableSubGraph.builder(graph);
		}
	}

	public static <T> ImmutableGraphAsDot.Builder<T> builder(Function<T, String> nodeAsString) {
		return ImmutableGraphAsDot.builder(nodeAsString);
	}
}
