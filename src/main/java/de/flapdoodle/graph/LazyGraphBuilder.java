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
