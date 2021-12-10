package de.flapdoodle.graph;

import org.immutables.builder.Builder;
import org.immutables.value.Value;
import org.jgrapht.Graph;

import java.util.Map;

public abstract class Vertex {

	public abstract String name();

	@Value.Immutable
	public static abstract class Named extends Vertex {

		@Override
		@Value.Parameter
		public abstract String name();

		public static Named of(String name) {
			return ImmutableNamed.of(name);
		}
	}

	@Value.Immutable
	public static abstract class WithGraph extends Vertex {

		@Override
		@Builder.Parameter
		public abstract String name();

		@Builder.Parameter
		public abstract Graph<Vertex,?> graph();

		public abstract Named in();

		public abstract Named out();

		public abstract Map<Named, Named> connections();

		public static ImmutableWithGraph.Builder of(String name, Graph<Vertex,?> graph) {
			return ImmutableWithGraph.builder(name,graph);
		}
	}
}
