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

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.AbstractGraphBuilder;
import org.jgrapht.graph.builder.DirectedGraphBuilder;

public class Graphs {

	public static <V,E> DirectedGraph<V, E> filter(DirectedGraph<V, E> src, Predicate<V> filter) {
		DefaultDirectedGraph<V, E> ret = new DefaultDirectedGraph<>(src.getEdgeFactory());
		
		src.vertexSet().forEach(v -> {
			if (filter.test(v)) {
				ret.addVertex(v);
				
				src.outgoingEdgesOf(v);
			}
		});
		
		return ret;
	}
	
	public static <V, E, G extends Graph<V, E>, B extends AbstractGraphBuilder<V, E, G, B>> WithGraphBuilder<V,E,G,B> with(Supplier<B> graphSupplier) {
		return new WithGraphBuilder<V,E,G,B>(graphSupplier);
	}
	
	
	public static class WithGraphBuilder<V, E, G extends Graph<V, E>, B extends AbstractGraphBuilder<V, E, G, B>> {
		
		private final Supplier<B> graphSupplier;

		public WithGraphBuilder(Supplier<B> graphSupplier) {
			this.graphSupplier = graphSupplier;
		}
		
		public <T> G build(Iterable<T> src, BiConsumer<? super B, T> forEach) {
			B ret = graphSupplier.get();
			
			src.forEach(t -> forEach.accept(ret, t));
			
			return ret.build();
		}
	}
	
	public static <V, E> Supplier<DirectedGraphBuilder<V, E, DefaultDirectedGraph<V, E>>> directedGraphBuilder(Class<V> vertexType, Class<? extends E> edgeClass) {
		return () -> new DirectedGraphBuilder<V,E,DefaultDirectedGraph<V, E>>(new DefaultDirectedGraph<V,E>(edgeClass));
	}
	
	public static <V> Supplier<DirectedGraphBuilder<V, DefaultEdge, DefaultDirectedGraph<V, DefaultEdge>>> directedGraphBuilder(Class<V> vertexType) {
		return directedGraphBuilder(vertexType, DefaultEdge.class);
	}
}
