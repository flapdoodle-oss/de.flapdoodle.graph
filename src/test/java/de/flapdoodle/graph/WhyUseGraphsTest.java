package de.flapdoodle.graph;

import de.flapdoodle.graph.doc.Base;
import de.flapdoodle.graph.doc.Embedded;
import de.flapdoodle.graph.doc.Named;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class WhyUseGraphsTest {
	@RegisterExtension
	public static Recording recording = Recorder.with("WhyUseGraphs.md", TabSize.spaces(2));

	@Test
	public void simpleGraph() {
		recording.begin("vertex");
		Named a = Named.of("A");
		Named b = Named.of("B");
		Named c = Named.of("C");

		Named embeddedA = Named.of("a");
		Named embeddedB = Named.of("b");
		recording.end();

		recording.begin("embedded");
		Embedded embedded = Embedded.builder()
			.name("embedded")
			.graph(GraphBuilder.<Base>withDirectedGraph()
				.addEdgeChain(embeddedA, embeddedB)
				.build())
			.putConnections(b, embeddedA)
			.putConnections(c, embeddedB)
			.build();
		recording.end();

		recording.begin("graph");
		DefaultDirectedGraph<Base, DefaultEdge> graph = GraphBuilder.<Base>withDirectedGraph()
			.addEdgeChain(a, b, embedded, c)
			.build();
		recording.end();

		recording.begin("dotFile");
		String dotFile = GraphAsDot.builder(Base::name)
			.subGraphIdSeparator("__")
			.label("label")
			.nodeAsLabel(vertex -> "label " + vertex.name())
			.nodeAttributes(vertex -> asMap("shape", "rectangle"))
			.sortedBy((GraphAsDot.AsComparable<Base, String>) Base::name)
			.subGraph(vertex -> vertex instanceof Embedded
				? Optional.of(((Embedded) vertex).subGraph())
				: Optional.empty())
			.build()
			.asDot(graph);
		recording.end();
		recording.output("copy-file.dot", dotFile);
		recording.file("copy-file.dot.svg", "WhyUseGraphs.svg", GraphvizAdapter.asSvg(dotFile));
	}

	private Map<String, String> asMap(String key, String value) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put(key,value);
		return map;
	}
}
