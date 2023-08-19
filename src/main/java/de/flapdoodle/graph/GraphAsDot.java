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

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Default;
import org.jgrapht.Graph;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
public abstract class GraphAsDot<T> {

	@Parameter
	public abstract Function<T, String> nodeAsId();

	@Default
	public String subGraphIdSeparator() {
		return ":";
	}

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
	
	public abstract Optional<AsComparable<T, ?>> sortedBy();

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

		Context<T> context = new Context<>(this, sb);
		render(context.render(graph, 1));

		sb.append("}\n");
		return sb.toString();
	}

	private static class Context<T> {
		private final GraphAsDot<T> root;
		private final StringBuilder sb;
		private final AtomicInteger clusterCounter = new AtomicInteger();

		public Context(GraphAsDot<T> root, StringBuilder sb) {
			this.root = root;
			this.sb = sb;
		}

		public <E> Render<E> render(Graph<T, E> graph, int level) {
			return new Render<>(this, graph, level, "");
		}

		private class Render<E> {

			private final Context<T> context;
			private final Graph<T, E> graph;
			private final int level;

			private final Map<T, VertexInSubGraph<T>> outerVertexToInnerVertexMap;
			private final int clusterId;
			private final String clusterPrefix;
			private final String indent;

			private Render(
				Context<T> context,
				Graph<T, E> graph,
				int level,
				String clusterPrefix
			) {
				this.context = context;
				this.graph = graph;
				this.level = level;
				this.clusterPrefix = clusterPrefix;
				this.clusterId = context.clusterCounter.getAndIncrement();
				this.indent = String.join("", Collections.nCopies(level, "\t"));

				List<Vertex2SubGraph<T>> subGraphs = graph.vertexSet().stream()
					.flatMap(v -> context.root.subGraph().apply(v)
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

				this.outerVertexToInnerVertexMap = outerVertexToInnerVertexList.stream()
					.collect(Collectors.toMap(v -> v.vertex, v -> v.vertexInSubGraph));
			}

			public Optional<Render<?>> subGraph(T v) {
				Optional<SubGraph<T>> subGraph = context.root.subGraph().apply(v);
				return subGraph.map(sg -> subGraph(sg.graph(), clusterPrefix(v)));
			}

			private String clusterPrefix(T cluster) {
				String separator = context.root.subGraphIdSeparator();
				String localPrefix = context.root.nodeAsId().apply(cluster);
				return clusterPrefix.isEmpty() ? localPrefix+separator : clusterPrefix+localPrefix+separator;
			}

			private <X> Render<X> subGraph(Graph<T, X> subGraph, String prefix) {
				return new Render<>(context, subGraph, level + 1, prefix);
			}

			public void newLine() {
				sb.append("\n");
			}

			public Render<E> line(String content) {
				sb.append(indent).append(content).append("\n");
				return this;
			}

			public void forEachEdge(BiConsumer<T, T> onEdge) {
				Consumer<E> edgeConsumer = edge -> {
					T start = graph.getEdgeSource(edge);
					T end = graph.getEdgeTarget(edge);
					onEdge.accept(start, end);
				};

				if (root.sortedBy().isPresent()) {
					graph.edgeSet().stream()
						.sorted(new MappingEdgeComparator<>(graph, root.sortedBy().get()))
						.forEach(edgeConsumer);
				} else graph.edgeSet()
					.forEach(edgeConsumer);
			}

			public boolean isNoSubGraph(T vertex) {
				return !context.root.subGraph().apply(vertex).isPresent();
			}

			private Render<E> connection(T a, T b) {
				renderConnection(clusterPrefix+root.nodeAsId().apply(a), clusterPrefix+root.nodeAsId().apply(b), root.edgeAttributes().apply(a, b), sb);
				return this;
			}

			public void subGraphConnection(T a, T b) {
//				System.out.println("subGraphConnection -> "+a+" --> "+b);
//				outerVertexToInnerVertexMap.forEach((key, value) -> {
//					System.out.println(key);
//					System.out.println("--> "+value);
//				});
				
				VertexInSubGraph<T> innerA = outerVertexToInnerVertexMap.get(a);
				VertexInSubGraph<T> innerB = outerVertexToInnerVertexMap.get(b);

				if (innerA != null) {
					String aId=clusterPrefix+root.nodeAsId().apply(a);
					String innerAId=clusterPrefix(innerA.parent)+root.nodeAsId().apply(innerA.vertex);
					renderConnection(aId, innerAId, root.edgeAttributes().apply(a, innerA.vertex), sb);
				}
				if (innerB != null) {
					String innerBId=clusterPrefix(innerB.parent)+root.nodeAsId().apply(innerB.vertex);
					String bId=clusterPrefix+root.nodeAsId().apply(b);

					renderConnection(innerBId, bId, root.edgeAttributes().apply(innerB.vertex, b), sb);
				}
				if (innerA==null && innerB==null) throw new IllegalArgumentException("could not find mapping for "+a+" or "+b+" in "+outerVertexToInnerVertexMap);
			}

			public void forEachVertex(Consumer<T> onVertex) {
				if (root.sortedBy().isPresent()) {
					graph.vertexSet().stream()
						.sorted(new MappingComparator<>(root.sortedBy().get()))
						.forEach(onVertex);
				} else
					graph.vertexSet()
						.forEach(onVertex);
			}

			public void renderNode(T v) {
				String id = clusterPrefix + context.root.nodeAsId().apply(v);
				String label = context.root.nodeAsLabel().apply(v);
				Map<String, String> attributes = context.root.nodeAttributes().apply(v);
				line(quote(id) + asNodeAttributes(id.equals(label) ? attributes : withLabel(attributes, label)) + ";");
			}

			private Map<String, String> withLabel(Map<String, String> attributes, String label) {
				LinkedHashMap<String, String> copy = new LinkedHashMap<>(attributes);
				if (!copy.containsKey("label")) {
					copy.put("label", label);
				}
				return copy;
			}

			private void renderConnection(String a, String b, Map<String, String> edgeAttributes, StringBuilder sb) {
				sb.append(indent);
				sb.append(quote(a))
					.append(" -> ")
					.append(quote(b))
					.append(asNodeAttributes(edgeAttributes)).append(";\n");
			}
		}
	}

	private <E> void render(Context<T>.Render<E> context) {
		renderNodes(context);
		context.newLine();
		renderEdges(context);
	}

	private <E> void renderNodes(Context<T>.Render<E> context) {
		context.forEachVertex(v -> {
			Optional<Context<T>.Render<?>> subContext = context.subGraph(v);
			if (subContext.isPresent()) {
				context.line("subgraph cluster_" + subContext.get().clusterId + " {");
				subContext.get().line("label = "+quote(context.context.root.nodeAsLabel().apply(v))+";");
				render(subContext.get());
				context.line("}");
			} else {
				context.renderNode(v);
			}
		});
	}

	private <E> void renderEdges(Context<T>.Render<E> context) {
		context.forEachEdge((a, b) -> {
			if (context.isNoSubGraph(a) && context.isNoSubGraph(b)) {
				context.connection(a, b);
			} else {
				context.subGraphConnection(a, b);
			}
		});
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

		@Override
		public String toString() {
			return "VertexInSubGraph{" +
				"parent=" + parent +
				", vertex=" + vertex +
				'}';
		}
	}

	public static <T> ImmutableGraphAsDot.Builder<T> builder(Function<T, String> nodeAsId) {
		return ImmutableGraphAsDot.builder(nodeAsId);
	}

	@FunctionalInterface
	public interface AsComparable<T, C extends Comparable<C>> {
		C map(T value);
	}

	private static class MappingComparator<T, C extends Comparable<C>> implements Comparator<T> {

		private final AsComparable<T, C> mapping;

		private MappingComparator(AsComparable<T, C> mapping) {
			this.mapping = mapping;
		}

		@Override
		public int compare(T first, T second) {
			return mapping.map(first).compareTo(mapping.map(second));
		}
	}

	private static class MappingEdgeComparator<T, E, C extends Comparable<C>> implements Comparator<E> {

		private final Graph<T, E> graph;
		private final MappingComparator<T, C> comparator;

		public MappingEdgeComparator(Graph<T, E> graph, AsComparable<T, C> asComparable) {
			this.graph = graph;
			this.comparator = new MappingComparator<>(asComparable);
		}

		@Override
		public int compare(E first, E second) {
			int compareFirst = comparator.compare(graph.getEdgeSource(first), graph.getEdgeSource(second));

			return compareFirst == 0
				? comparator.compare(graph.getEdgeTarget(first), graph.getEdgeTarget(second))
				: compareFirst;
		}
	}
}
