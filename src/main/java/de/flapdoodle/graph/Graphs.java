/**
 * Copyright (C) 2013
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

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedSubgraph;

import de.flapdoodle.graph.ImmutableVerticesAndEdges.Builder;

public class Graphs {

	public static <V,E> DirectedGraph<V, E> filter(DirectedGraph<V, E> src, Predicate<V> filter) {
		return filter(src,filter,v -> {}, edge -> {});
	}
	
	public static <V,E> DirectedGraph<V, E> filter(DirectedGraph<V, E> src, Predicate<V> filter, Consumer<V> filteredVertexConsumer, Consumer<E> filteredEdgeConsumer) {
		DefaultDirectedGraph<V, E> ret = new DefaultDirectedGraph<>(src.getEdgeFactory());
		
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
	
	public static <V,E> Collection<VerticesAndEdges<V, E>> leavesOf(DirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, true);
	}
	
	public static <V,E> Collection<VerticesAndEdges<V, E>> rootsOf(DirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, false);
	}
	
	private static <V,E> Collection<VerticesAndEdges<V, E>> leavesOrRootsOf(DirectedGraph<V, E> src,boolean leafes) {
		List<VerticesAndEdges<V,E>> ret=new ArrayList<>();

		Builder<V, E> builder = ImmutableVerticesAndEdges.builder();
		
//		System.out.println("----------------------");
//		System.out.println(GraphAsDot.<V>builder(s -> s.toString())
//			.build().asDot(src));
		
		DirectedGraph<V, E> filtered = filter(src, leafes ? isLeaf(src).negate() : isRoot(src).negate(), t -> builder.addVertices(t), e -> builder.addEdges(ImmutableEdge.of(src.getEdgeSource(e), src.getEdgeTarget(e), e)));
		
		ImmutableVerticesAndEdges<V, E> verticesAndEdges = builder.build();
		
//		System.out.println("========================");
//		System.out.println(verticesAndEdges);
		
		if (!verticesAndEdges.vertices().isEmpty()) {
			ret.add(verticesAndEdges);
			ret.addAll(leavesOrRootsOf(filtered, leafes));
		} else {
	        StrongConnectivityAlgorithm<V, E> inspector =
	                new KosarajuStrongConnectivityInspector<>(src);
	        List<DirectedSubgraph<V, E>> loopingSubGraph = inspector.stronglyConnectedSubgraphs()
	        		.stream()
	        		.filter(l -> l.vertexSet().size()>1 || l.containsEdge(l.vertexSet().iterator().next(), l.vertexSet().iterator().next()))
	        		.collect(Collectors.toList());
	        
	        Set<V> vertexInLoopSet = loopingSubGraph.stream()
	        		.flatMap(g -> g.vertexSet().stream())
	        		.collect(Collectors.toSet());
	        
//	        loopingSubGraph.forEach(l -> {
//				System.out.println("~Loop~~~~~~");
//				System.out.println(GraphAsDot.<V>builder(s -> s.toString())
//					.build().asDot(l));
//	        });
//	        
//	        System.out.println("~In Loop: "+vertexInLoopSet);
	        
	        if (!loopingSubGraph.isEmpty()) {
	    		Builder<V, E> loopingVerticesAndEdgesBuilder = ImmutableVerticesAndEdges.builder();
	        	DirectedGraph<V, E> filteredFromLoops = filter(filtered, v -> !vertexInLoopSet.contains(v), t -> loopingVerticesAndEdgesBuilder.addVertices(t), e -> {});
	        	
	        	loopingSubGraph.forEach(g -> {
	        		ImmutableLoop.Builder<V, E> loopBuilder=ImmutableLoop.builder();
	        		g.edgeSet().forEach(egde -> {
	        			loopBuilder.addEdges(ImmutableEdge.of(filtered.getEdgeSource(egde), filtered.getEdgeTarget(egde), egde));
	        		});
	        		loopingVerticesAndEdgesBuilder.addLoops(loopBuilder.build());
	        	});
	        	
				ImmutableVerticesAndEdges<V, E> loopingVerticesAndEdges = loopingVerticesAndEdgesBuilder.build();
//				System.out.println("=L======================");
//				System.out.println(loopingVerticesAndEdges);
				
				ret.add(loopingVerticesAndEdges);
				ret.addAll(leavesOrRootsOf(filteredFromLoops, leafes));
	        }
		}
		return Collections.unmodifiableCollection(ret);
	}

	
	public static <V> Predicate<V> isLeaf(DirectedGraph<V, ?> graph) {
		return v -> graph.outDegreeOf(v) == 0;
	}
	
	public static <V> Predicate<V> isRoot(DirectedGraph<V, ?> graph) {
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
		return () -> new GraphBuilder<V,E,G>(graphSupplier.get());
	}

//	public static <V, E, G extends DirectedGraph<V, E>> Supplier<DirectedGraphBuilder<V, E, G>> directedGraphBuilder(Supplier<G> graphSupplier) {
//		return () -> new DirectedGraphBuilder<V,E,G>(graphSupplier.get());
//	}
//	
//	public static <V> Supplier<DirectedGraphBuilder<V, DefaultEdge, DefaultDirectedGraph<V, DefaultEdge>>> directedGraphBuilder() {
//		return Graphs.directedGraphBuilder(Graphs.directedGraph(DefaultEdge.class));
//	}

	public static <V> Supplier<DefaultDirectedGraph<V, DefaultEdge>> directedGraph() {
		return directedGraph(DefaultEdge.class);
	}

	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraph(Class<? extends E> edgeClass) {
		return () -> new DefaultDirectedGraph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return () -> new DefaultDirectedGraph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DirectedMultigraph<V, DefaultEdge>> directedMultiEdgeGraph() {
		return directedMultiEdgeGraph(DefaultEdge.class);
	}
	
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraph(Class<? extends E> edgeClass) {
		return () -> new DirectedMultigraph<V,E>(edgeClass);
	}
	
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return () -> new DirectedMultigraph<V,E>(edgeClass);
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

		public GraphBuilder<V,E,G> addVertices(V a, V b, V ... other) {
			graph.addVertex(a);
			graph.addVertex(b);
			for (V o : other) {
				graph.addVertex(o);
			}
			return this;
		}

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
	}
}
