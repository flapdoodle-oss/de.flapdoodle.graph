package de.flapdoodle.graph.doc;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.ImmutableSubGraph;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Map;

@Value.Immutable
public interface Embedded extends Base {
	Map<Base, Base> connections();
	Graph<Base, DefaultEdge> graph();

	@Value.Auxiliary
	default GraphAsDot.SubGraph<Base> subGraph() {
		return ImmutableSubGraph.builder(graph())
			.putAllConnections(connections())
			.build();
	}

	static ImmutableEmbedded.Builder builder() {
		return ImmutableEmbedded.builder();
	}
}
