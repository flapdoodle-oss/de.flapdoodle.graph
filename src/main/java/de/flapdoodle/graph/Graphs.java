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
import org.jgrapht.graph.builder.AbstractGraphBuilder;
import org.jgrapht.graph.builder.DirectedGraphBuilder;

public class Graphs {

	public static <T> WithIterable<T> with(Iterable<T> src) {
		return new WithIterable<>(src);
	}
	
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
	

	public static class WithIterable<T> {
		
		private final Iterable<T> src;

		public WithIterable(Iterable<T> src) {
			this.src = src;
		}
		
		public <V, E, G extends Graph<V, E>, B extends AbstractGraphBuilder<V, E, G, B>> AndGraphBuilder<T,V,E,G,B> graphBuilder(Supplier<B> graphSupplier) {
			return new AndGraphBuilder<T,V,E,G,B>(src,graphSupplier);
		}
	}
	
	public static class AndGraphBuilder<T, V, E, G extends Graph<V, E>, B extends AbstractGraphBuilder<V, E, G, B>> {

		private final Iterable<T> src;
		private final Supplier<B> graphSupplier;

		public AndGraphBuilder(Iterable<T> src, Supplier<B> graphSupplier) {
			this.src = src;
			this.graphSupplier = graphSupplier;
		}
		
		public G build(BiConsumer<? super B, T> forEach) {
			B ret = graphSupplier.get();
			
			src.forEach(t -> forEach.accept(ret, t));
			
			return ret.build();
		}
	}
	
	public static <V, E> Supplier<DirectedGraphBuilder<V, E, DefaultDirectedGraph<V, E>>> directedGraphBuilder(Class<? extends E> edgeClass) {
		return () -> new DirectedGraphBuilder<V,E,DefaultDirectedGraph<V, E>>(new DefaultDirectedGraph<V,E>(edgeClass));
	}
}
