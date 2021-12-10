package de.flapdoodle.graph;

import org.immutables.value.Value;
import org.jgrapht.Graph;

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
		@Value.Parameter
		public abstract String name();

		@Value.Parameter
		public abstract Graph<Vertex,?> graph();

		public static WithGraph of(String name, Graph<Vertex,?> graph) {
			return ImmutableWithGraph.of(name,graph);
		}
	}
}
