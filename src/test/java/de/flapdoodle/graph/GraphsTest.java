package de.flapdoodle.graph;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

public class GraphsTest {

	@Test
	public void filterGraph() {
		List<Integer> src = Stream.of(1,2,3,4,5,6,7)
				.collect(Collectors.toList());
		
		DefaultDirectedGraph<Integer, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(Integer.class))
				.build(src, (graphBuilder,b) -> {
					graphBuilder.addVertex(b);
					if (!b.equals(1)) {
						graphBuilder.addEdge(b, 1);
					}
				});
		
		assertEquals("[1, 2, 3, 4, 5, 6, 7]", graph.vertexSet().toString());
		assertEquals("[(2 : 1), (3 : 1), (4 : 1), (5 : 1), (6 : 1), (7 : 1)]", graph.edgeSet().toString());
		
		DirectedGraph<Integer, DefaultEdge> filtered = Graphs.filter(graph, v -> v % 2 !=0);
		
		assertEquals("[1, 3, 5, 7]", filtered.vertexSet().toString());
		assertEquals("[(3 : 1), (5 : 1), (7 : 1)]", filtered.edgeSet().toString());
	}
	
	@Test
	public void collectionOfLeaves() {
		DefaultDirectedGraph<String, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(String.class))
			.build(builder -> {
				builder.addVertices("A","B","C","Ca","D","Da","Db","Dc");
				builder.addEdgeChain("A", "B", "C", "D");
				builder.addEdge("C","Ca");
				builder.addEdge("D","Da");
				builder.addEdge("D","Db");
				builder.addEdge("D","Dc");
			});
		
		Collection<VerticesAndEdges<String, DefaultEdge>> leavesSet = Graphs.leavesOf(graph);
		
		assertEquals(5, leavesSet.size());
		
		Iterator<VerticesAndEdges<String, DefaultEdge>> iterator = leavesSet.iterator();
		VerticesAndEdges<String, DefaultEdge> set = iterator.next();
		
		assertEquals("[Ca, Da, Db, Dc]",set.vertices().toString());
		assertEquals("[Edge{start=C, end=Ca, edge=(C : Ca)}, Edge{start=D, end=Da, edge=(D : Da)}, Edge{start=D, end=Db, edge=(D : Db)}, Edge{start=D, end=Dc, edge=(D : Dc)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[D]",set.vertices().toString());
		assertEquals("[Edge{start=C, end=D, edge=(C : D)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[C]",set.vertices().toString());
		assertEquals("[Edge{start=B, end=C, edge=(B : C)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[B]",set.vertices().toString());
		assertEquals("[Edge{start=A, end=B, edge=(A : B)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[A]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
	}
	
	@Test
	public void collectionOfLeavesWithCycles() {
		DefaultDirectedGraph<String, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(String.class))
			.build(builder -> {
				builder.addVertices("A","B","C","Ca","D","Da","Db","Dc");
				builder.addEdgeChain("A", "B", "C", "D");
				builder.addEdge("C","Ca");
				builder.addEdge("D","Da");
				builder.addEdge("D","Db");
				builder.addEdge("D","Dc");
				builder.addEdge("Dc","C");
			});
		
		Collection<VerticesAndEdges<String, DefaultEdge>> leavesSet = Graphs.leavesOf(graph);
		
		assertEquals(4, leavesSet.size());
		
		Iterator<VerticesAndEdges<String, DefaultEdge>> iterator = leavesSet.iterator();
		VerticesAndEdges<String, DefaultEdge> set = iterator.next();
		
		assertEquals("[Ca, Da, Db]",set.vertices().toString());
		assertEquals("[Edge{start=C, end=Ca, edge=(C : Ca)}, Edge{start=D, end=Da, edge=(D : Da)}, Edge{start=D, end=Db, edge=(D : Db)}]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[C, D, Dc]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
		assertEquals("[Loop{edges=[Edge{start=C, end=D, edge=(C : D)}, Edge{start=D, end=Dc, edge=(D : Dc)}, Edge{start=Dc, end=C, edge=(Dc : C)}]}]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[B]",set.vertices().toString());
		assertEquals("[Edge{start=A, end=B, edge=(A : B)}]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[A]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
	}
	
	@Test
	public void collectionOfRoots() {
		DefaultDirectedGraph<String, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(String.class))
			.build(builder -> {
				builder.addVertices("A","B","C","Ca","D","Da","Db","Dc");
				builder.addEdgeChain("A", "B", "C", "D");
				builder.addEdge("C","Ca");
				builder.addEdge("D","Da");
				builder.addEdge("D","Db");
				builder.addEdge("D","Dc");
			});
		
		Collection<VerticesAndEdges<String, DefaultEdge>> leavesSet = Graphs.rootsOf(graph);
		
		assertEquals(5, leavesSet.size());
		
		Iterator<VerticesAndEdges<String, DefaultEdge>> iterator = leavesSet.iterator();
		VerticesAndEdges<String, DefaultEdge> set;
		
		set = iterator.next();
		assertEquals("[A]",set.vertices().toString());
		assertEquals("[Edge{start=A, end=B, edge=(A : B)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[B]",set.vertices().toString());
		assertEquals("[Edge{start=B, end=C, edge=(B : C)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[C]",set.vertices().toString());
		assertEquals("[Edge{start=C, end=D, edge=(C : D)}, Edge{start=C, end=Ca, edge=(C : Ca)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[Ca, D]",set.vertices().toString());
		assertEquals("[Edge{start=D, end=Da, edge=(D : Da)}, Edge{start=D, end=Db, edge=(D : Db)}, Edge{start=D, end=Dc, edge=(D : Dc)}]",set.edges().toString());
		
		set = iterator.next();
		assertEquals("[Da, Db, Dc]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
		
	}

	@Test
	public void collectionOfRootsWithCycles() {
		DefaultDirectedGraph<String, DefaultEdge> graph = Graphs.with(Graphs.directedGraphBuilder(String.class))
			.build(builder -> {
				builder.addVertices("A","B","C","Ca","D","Da","Db","Dc");
				builder.addEdgeChain("A", "B", "C", "D");
				builder.addEdge("C","Ca");
				builder.addEdge("D","Da");
				builder.addEdge("D","Db");
				builder.addEdge("D","Dc");
				builder.addEdge("Dc","C");
			});
		
		Collection<VerticesAndEdges<String, DefaultEdge>> leavesSet = Graphs.rootsOf(graph);
		
		assertEquals(4, leavesSet.size());
		
		Iterator<VerticesAndEdges<String, DefaultEdge>> iterator = leavesSet.iterator();
		VerticesAndEdges<String, DefaultEdge> set;
		
		set = iterator.next();
		assertEquals("[A]",set.vertices().toString());
		assertEquals("[Edge{start=A, end=B, edge=(A : B)}]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[B]",set.vertices().toString());
		assertEquals("[Edge{start=B, end=C, edge=(B : C)}]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[C, D, Dc]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
		assertEquals("[Loop{edges=[Edge{start=C, end=D, edge=(C : D)}, Edge{start=D, end=Dc, edge=(D : Dc)}, Edge{start=Dc, end=C, edge=(Dc : C)}]}]",set.loops().toString());
		
		set = iterator.next();
		assertEquals("[Ca, Da, Db]",set.vertices().toString());
		assertEquals("[]",set.edges().toString());
		assertEquals("[]",set.loops().toString());
	}
}
