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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
	
	public static <V,E> List<VerticesAndEdges<V, E>> leavesOf(DefaultDirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, true);
	}
	
	public static <V,E> List<VerticesAndEdges<V, E>> rootsOf(DefaultDirectedGraph<V, E> src) {
		return leavesOrRootsOf(src, false);
	}
	
	private static <V,E> List<VerticesAndEdges<V, E>> leavesOrRootsOf(DefaultDirectedGraph<V, E> src,boolean leafes) {
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
		return Collections.unmodifiableList(ret);
	}

	private static <V, E> ImmutableVerticesAndEdges<V, E> verticesAndEdgesOf(List<? extends Loop<V, E>> loops) {
		Builder<V, E> loopingVerticesAndEdgesBuilder = ImmutableVerticesAndEdges.builder();
		
		loops.forEach(l -> {
			loopingVerticesAndEdgesBuilder.addAllVertices(l.vertexSet());
		});
		loopingVerticesAndEdgesBuilder.addAllLoops(loops);

		return loopingVerticesAndEdgesBuilder.build();
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

	public static <V> boolean hasPath(DefaultDirectedGraph<V, ?> graph, V from, V to) {
		GraphPath<V, ?> paths = DijkstraShortestPath.findPathBetween(graph, from, to);
		return paths!=null && !paths.getEdgeList().isEmpty();
	}

	public static <V, E, G extends Graph<V, E>> LazyGraphBuilder<V,E,G> with(Supplier<GraphBuilder<V,E,G>> graphSupplier) {
		return new LazyGraphBuilder<V,E,G>(graphSupplier);
	}

	public static <V, E, G extends Graph<V, E>> Supplier<GraphBuilder<V, E, G>> graphBuilder(Supplier<G> graphSupplier) {
		return () -> GraphBuilder.of(graphSupplier.get());
	}
	
	public static <V> Supplier<GraphBuilder<V, DefaultEdge, DefaultDirectedGraph<V, DefaultEdge>>> directedGraphBuilder() {
		return () -> GraphBuilder.of(Graphs.<V>directedGraph());
	}

	@Deprecated
	public static <V> Supplier<DefaultDirectedGraph<V, DefaultEdge>> directedGraphFactory() {
		return Directed::newInstance;
	}

	@Deprecated
	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraphFactory(Class<? extends E> edgeClass) {
		return Directed.factory(edgeClass);
	}

	@Deprecated
	public static <V> DefaultDirectedGraph<V, DefaultEdge> directedGraph() {
		return Directed.newInstance(DefaultEdge.class);
	}

	@Deprecated
	public static <V, E> DefaultDirectedGraph<V, E> directedGraph(Class<? extends E> edgeClass) {
		return Directed.newInstance(edgeClass);
	}

	@Deprecated
	public static <V, E> Supplier<DefaultDirectedGraph<V, E>> directedGraphFactory(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return Directed.factory(edgeClass);
	}
	
	@Deprecated
	public static <V, E> Supplier<DirectedMultigraph<V, DefaultEdge>> directedMultiEdgeGraphFactory() {
		return Multi.factory(DefaultEdge.class);
	}

	@Deprecated
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraphFactory(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return Multi.factory(edgeClass);
	}

	@Deprecated
	public static <V, E> Supplier<DirectedMultigraph<V, E>> directedMultiEdgeGraphFactory(Class<? extends E> edgeClass) {
		return Multi.factory(edgeClass);
	}

	@Deprecated
	private static <V> DirectedMultigraph<V, DefaultEdge> directedMultiEdgeGraph() {
		return Multi.newInstance(DefaultEdge.class);
	}

	@Deprecated
	private static <V, E> DirectedMultigraph<V, E> directedMultiEdgeGraph(Class<? extends E> edgeClass) {
		return Multi.newInstance(edgeClass);
	}

	@Deprecated
	public static <V, E> Supplier<DirectedPseudograph<V, DefaultEdge>> directedPseudoGraph() {
		return Pseudo.factory(DefaultEdge.class);
	}

	@Deprecated
	public static <V, E> Supplier<DirectedPseudograph<V, E>> directedPseudoGraph(Class<? extends E> edgeClass) {
		return Pseudo.factory(edgeClass);
	}

	@Deprecated
	public static <V, E> Supplier<DirectedPseudograph<V, E>> directedPseudoGraph(Class<V> vertexClass, Class<? extends E> edgeClass) {
		return Pseudo.factory(edgeClass);
	}

	public abstract static class Directed {
		public static <V, E> Supplier<DefaultDirectedGraph<V, E>> factory(Class<? extends E> edgeClass) {
			return () -> newInstance(edgeClass);
		}

		public static <V> DefaultDirectedGraph<V, DefaultEdge> newInstance() {
			return newInstance(DefaultEdge.class);
		}

		public static <V, E> DefaultDirectedGraph<V, E> newInstance(Class<? extends E> edgeClass) {
			return new DefaultDirectedGraph<V, E>(edgeClass);
		}
	}

	public abstract static class Multi {
		public static <V, E> Supplier<DirectedMultigraph<V, E>> factory(Class<? extends E> edgeClass) {
			return () -> newInstance(edgeClass);
		}

		public static <V> DirectedMultigraph<V, DefaultEdge> newInstance() {
			return newInstance(DefaultEdge.class);
		}

		public static <V, E> DirectedMultigraph<V, E> newInstance(Class<? extends E> edgeClass) {
			return new DirectedMultigraph<V, E>(edgeClass);
		}
	}

	public abstract static class Pseudo {
		public static <V, E> Supplier<DirectedPseudograph<V, E>> factory(Class<? extends E> edgeClass) {
			return () -> newInstance(edgeClass);
		}

		public static <V> DirectedPseudograph<V, DefaultEdge> newInstance() {
			return newInstance(DefaultEdge.class);
		}

		public static <V, E> DirectedPseudograph<V, E> newInstance(Class<? extends E> edgeClass) {
			return new DirectedPseudograph<V, E>(edgeClass);
		}
	}
}
