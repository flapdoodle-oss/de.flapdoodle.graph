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

import org.jgrapht.Graph;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LazyGraphBuilder<V, E, G extends Graph<V, E>> {

	private final Supplier<GraphBuilder<V, E, G>> graphSupplier;

	public LazyGraphBuilder(Supplier<GraphBuilder<V, E, G>> graphSupplier) {
		this.graphSupplier = graphSupplier;
	}

	public <T> G build(Iterable<T> src, BiConsumer<? super GraphBuilder<V, E, ?>, T> forEach) {
		GraphBuilder<V, E, G> ret = graphSupplier.get();

		src.forEach(t -> forEach.accept(ret, t));

		return ret.build();
	}

	public G build(Consumer<? super GraphBuilder<V, E, ?>> graphBuilderConsumer) {
		GraphBuilder<V, E, G> ret = graphSupplier.get();

		graphBuilderConsumer.accept(ret);

		return ret.build();
	}
}
