package de.flapdoodle.graph;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

public class GraphsTest {

	@Test
	public void filterGraph() {
		List<Integer> src = Stream.of(1,2,3,4,5,6,7)
				.collect(Collectors.toList());
		
		DefaultDirectedGraph<Integer, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(Integer.class))
				.build(src, (a,b) -> a.addVertex(b));
	}
}
