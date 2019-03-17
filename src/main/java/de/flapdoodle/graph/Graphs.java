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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedPseudograph;

import de.flapdoodle.graph.ImmutableVerticesAndEdges.Builder;

public class Graphs {

	public static <V,E> DefaultDirectedGraph<V, E> filter(DefaultDirectedGraph<V, E> src, Predicate<V> filter) {
		return filter(src,filter,v -> {}, edge -> {});
	}
	
	public static <V,E> DefaultDirectedGraph<V, E> filter(DefaultDirectedGraph<V, E> src, Predicate<V> filter, Consumer<V> filteredVertexConsumer, Consumer<E> filteredEdgeConsumer) {
		DefaultDirectedGraph<V, E> ret = new DefaultDirectedGraph<>(src.getVertexSupplier(), src.getEdgeSupplier(), src.getType().isWeighted());
		
		src.vertexSet().forEach(v -> {
			if (filter.test(v)) {
				ret.addVertex(v);
			} else {
				filteredVertexConsumer.accept(v);
			}
		});
		
		src.edgeSet().forEach(edge -> {
			V source = src.getEdgeSource(edge);
			V target = src.getEdgeTarget(edge);
			if (filter.test(source) && filter.test(target)) {
				ret.addEdge(source, target, edge);
			} else {
				filteredEdgeConsumer.accept(edge);
			}
		});
		
		return ret;
	}
	
	public static <V,E> Collection<VerticesAndEdges<V, E>> leavesOf(DefaultDirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, true);
	}
	
	public static <V,E> Collection<VerticesAndEdges<V, E>> rootsOf(DefaultDirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, false);
	}
	
	private static <V,E> Collection<VerticesAndEdges<V, E>> leavesOrRootsOf(DefaultDirectedGraph<V, E> src,boolean leafes) {
		List<VerticesAndEdges<V,E>> ret=new ArrayList<>();

		Builder<V, E> builder = ImmutableVerticesAndEdges.builder();
		
		DefaultDirectedGraph<V, E> filtered = filter(src, leafes ? isLeaf(src).negate() : isRoot(src).negate(), t -> builder.addVertices(t), e -> builder.addEdges(ImmutableEdge.of(src.getEdgeSource(e), src.getEdgeTarget(e), e)));
		
		ImmutableVerticesAndEdges<V, E> verticesAndEdges = builder.build();
		
		if (!verticesAndEdges.vertices().isEmpty()) {
			ret.add(verticesAndEdges);
			ret.addAll(leavesOrRootsOf(filtered, leafes));
		} else {
	        List<Graph<V, E>> loopingSubGraph = loopsOfGraph(src);
	        
	        Set<V> vertexInLoopSet = loopingSubGraph.stream()
	        		.flatMap(g -> g.vertexSet().stream())
	        		.collect(Collectors.toSet());
	        
	        if (!loopingSubGraph.isEmpty()) {
	        	DefaultDirectedGraph<V, E> filteredFromLoops = filter(filtered, v -> !vertexInLoopSet.contains(v), t -> {}, e -> {});
	    		
	        	ImmutableVerticesAndEdges<V, E> loopingVerticesAndEdges = verticesAndEdgesOf(loopsOf(loopingSubGraph));
				
				ret.add(loopingVerticesAndEdges);
				ret.addAll(leavesOrRootsOf(filteredFromLoops, leafes));
	        }
		}
		return Collections.unmodifiableCollection(ret);
	}

	private static <V, E> ImmutableVerticesAndEdges<V, E> verticesAndEdgesOf(List<? extends Loop<V, E>> loops) {
		Builder<V, E> loopingVerticesAndEdgesBuilder = ImmutableVerticesAndEdges.builder();
		
		loops.forEach(l -> {
			loopingVerticesAndEdgesBuilder.addAllVertices(l.vertexSet());
		});
		loopingVerticesAndEdgesBuilder.addAllLoops(loops);
		
		ImmutableVerticesAndEdges<V, E> loopingVerticesAndEdges = loopingVerticesAndEdgesBuilder.build();
		return loopingVerticesAndEdges;
	}

	private static <V, E> List<? extends Loop<V, E>> loopsOf(List<Graph<V, E>> loopingSubGraph) {
		List<Loop<V, E>> ret=new ArrayList<>();
		
		loopingSubGraph.forEach(g -> {
			ImmutableLoop.Builder<V, E> loopBuilder=ImmutableLoop.builder();
			g.edgeSet().forEach(egde -> {
				loopBuilder.addEdges(ImmutableEdge.of(g.getEdgeSource(egde), g.getEdgeTarget(egde), egde));
			});
			ret.add(loopBuilder.build());
		});
		
		return Collections.unmodifiableList(ret);
	}

	private static <V, E> List<Graph<V, E>> loopsOfGraph(DefaultDirectedGraph<V, E> src) {
		StrongConnectivityAlgorithm<V, E> inspector =	new KosarajuStrongConnectivityInspector<>(src);
		
		List<Graph<V, E>> loopingSubGraph = inspector.getStronglyConnectedComponents()
				.stream()
				.filter(l -> l.vertexSet().size()>1 || l.containsEdge(l.vertexSet().iterator().next(), l.vertexSet().iterator().next()))
				.collect(Collectors.toList());
		
		return Collections.unmodifiableList(loopingSubGraph);
	}
	
	public static <V, E> List<? extends Loop<V, E>> loopsOf(DefaultDirectedGraph<V, E> src) {
		return loopsOf(loopsOfGraph(src));
	}

	
	public static <V> Predicate<V> isLeaf(DefaultDirectedGraph<V, ?> graph) {
		return v -> graph.outDegreeOf(v) == 0;
	}
	
	public static <V> Predicate<V> isRoot(DefaultDirectedGraph<V, ?> graph) {
		return v -> graph.inDegreeOf(v) == 0;
	}
	
	
	public static <V, E, G extends Graph<V, E>> WithGraphBuilder<V,E,G> with(Supplier<GraphBuilder<V,E,G>> graphSupplier) {
		return new WithGraphBuilder<V,E,G>(graphSupplier);
	}
	
	
	public static class WithGraphBuilder<V, E, G extends Graph<V, E>> {
		
		private final Supplier<GraphBuilder<V,E,G>> graphSupplier;

		public WithGraphBuilder(Supplier<GraphBuilder<V,E,G>> graphSupplier) {
			this.graphSupplier = graphSupplier;
		}
		
		public <T> G build(Iterable<T> src, BiConsumer<? super GraphBuilder<V,E,?>, T> forEach) {
			GraphBuilder<V, E, G> ret = graphSupplier.get();
			
			src.forEach(t -> forEach.accept(ret, t));
			
			return ret.build();
		}
		
		public G build(Consumer<? super GraphBuilder<V,E,?>> graphBuilderConsumer) {
			GraphBuilder<V, E, G> ret = graphSupplier.get();
			
			graphBuilderConsumer.accept(ret);
			
			return ret.build();
		}
	}
	
	public static <V, E, G extends Graph<V, E>> Supplier<GraphBuilder<V, E, G>> graphBuilder(Supplier<G> graphSupplier) {
		return () -> GraphBuilder.of(graphSupplier.get());
	}
	
	public static <V> Supplier<GraphBuilder<V, DefaultEdge, DefaultDirectedGraph<V, DefaultEdge>>> directedGraphBuilder() {
		return () -> GraphBuilder.of(Graphs.<V>directedGraph().get());
	}


	public static <V> Supplier<DefaultDirectedGraph<V, DefaultEdge>> directedGraph() {
		return directedGraph(DefaultEdge.class);
	}

	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraph(Class<? extends E> edgeClass) {
		return () -> new DefaultDirectedGraph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return directedGraph(edgeClass);
	}
	
	
	public static <V, E> Supplier<DirectedMultigraph<V, DefaultEdge>> directedMultiEdgeGraph() {
		return directedMultiEdgeGraph(DefaultEdge.class);
	}
	
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraph(Class<? extends E> edgeClass) {
		return () -> new DirectedMultigraph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return directedMultiEdgeGraph(edgeClass);
	}
	
	
	public static <V, E> Supplier<DirectedPseudograph<V, DefaultEdge>> directedPseudoGraph() {
		return directedPseudoGraph(DefaultEdge.class);
	}
	
	public static <V, E> Supplier<DirectedPseudograph<V, E>> directedPseudoGraph(Class<? extends E> edgeClass) {
		return () -> new DirectedPseudograph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DirectedPseudograph<V, E>> directedPseudoGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return directedPseudoGraph(edgeClass);
	}
	
	public static class GraphBuilder<V,E, G extends Graph<V, E>> {

		private final G graph;

		public GraphBuilder(G graph) {
			this.graph = graph;
		}

		public G build() {
			return graph;
		}

		public GraphBuilder<V,E,G> addVertex(V v) {
			graph.addVertex(v);
			return this;
		}

		public GraphBuilder<V,E,G> addEdge(V a, V b) {
			graph.addEdge(a, b);
			return this;
		}
		
		public GraphBuilder<V,E,G> addEdge(V a, V b, E edge) {
			graph.addEdge(a, b, edge);
			return this;
		}

		@SuppressWarnings("unchecked")
		public GraphBuilder<V,E,G> addVertices(V a, V b, V ... other) {
			graph.addVertex(a);
			graph.addVertex(b);
			for (V o : other) {
				graph.addVertex(o);
			}
			return this;
		}

		@SuppressWarnings("unchecked")
		public GraphBuilder<V,E,G> addEdgeChain(V a, V b, V ... other) {
			addVertices(a, b, other);
			
			graph.addEdge(a,b);
			V last=b;
			for (V o : other) {
				graph.addEdge(last,o);
				last=o;
			}
			return this;
		}
		
		private static <V,E,G extends Graph<V, E>> GraphBuilder<V, E, G> of(G graph) {
			return new GraphBuilder<V, E, G>(graph);
		}
	}
	
	public static <V> boolean hasPath(DefaultDirectedGraph<V, ?> graph, V from, V to) {
		GraphPath<V, ?> paths = DijkstraShortestPath.findPathBetween(graph, from, to);
		return paths!=null && !paths.getEdgeList().isEmpty();
	}
}
