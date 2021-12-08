package de.flapdoodle.graph;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class GraphAsDotTest {

	@Test
	void simpleGraphAsDot() {
		DefaultDirectedGraph<String, DefaultEdge> graph = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(String.class, DefaultEdge.class)))
			.build(builder -> {
				builder.addVertices("A","B","C");
				builder.addEdge("A","B");
				builder.addEdge("C","B");
			});

		String dotFile = GraphAsDot.builder(Function.identity())
			.build()
			.asDot(graph);

		assertThat(dotFile)
			.isEqualTo(dotFile("simple.dot"));
	}

	private static String dotFile(String name) {
		try {
			URL resource = GraphAsDotTest.class.getResource(name);
			if (resource==null) throw new RuntimeException("could not find "+name);
			Path path = Paths.get(resource.toURI());
			return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		}
		catch (URISyntaxException | IOException e) {
			throw new RuntimeException("could not load "+name, e);
		}
	}
}