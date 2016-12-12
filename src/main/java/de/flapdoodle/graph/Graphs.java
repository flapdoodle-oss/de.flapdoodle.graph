package de.flapdoodle.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class Graphs {

	public static <T> Builder<T,DefaultEdge,DefaultDirectedGraph<T,DefaultEdge>> directedGraph() {
		return new Builder<>(new DefaultDirectedGraph<>(DefaultEdge.class));
	}
	
	public static class Builder<V,E,T extends Graph<V, E>> {
		
		private final T graph;

		public Builder(T graph) {
			this.graph = graph;
		}

		
		
	}
}
