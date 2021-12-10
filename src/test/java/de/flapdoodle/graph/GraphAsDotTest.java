/**
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
import java.util.Optional;
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


	@Test
	void subGraphAsDot() {
		DefaultDirectedGraph<Vertex, DefaultEdge> sub = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(Vertex.class, DefaultEdge.class)))
			.build(builder -> {
				Vertex.Named x = Vertex.Named.of("x");
				Vertex.Named y = Vertex.Named.of("y");
				Vertex.Named z = Vertex.Named.of("z");
				builder.addVertices(x, y, z);
				builder.addEdge(x, y);
				builder.addEdge(y, z);
			});

		ImmutableWithGraph subGraph = Vertex.WithGraph.of("B", sub)
			.in(Vertex.Named.of("B.in"))
			.out(Vertex.Named.of("B.out"))
			.putConnections(Vertex.Named.of("B.in"), Vertex.Named.of("x"))
			.putConnections(Vertex.Named.of("B.out"), Vertex.Named.of("z"))
			.build();

		DefaultDirectedGraph<Vertex, DefaultEdge> graph = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(Vertex.class, DefaultEdge.class)))
			.build(builder -> {
				Vertex.Named a = Vertex.Named.of("A");
				Vertex.WithGraph b = subGraph;
				Vertex.Named c = Vertex.Named.of("C");

				builder.addVertices(a, b.in(), b, b.out(), c);
				builder.addEdge(a, b.in());
				builder.addEdge(b.in(), b);
				builder.addEdge(b, b.out());
				builder.addEdge(b.out(), c);
				
			});

		String dotFile = GraphAsDot.<Vertex>builder(Vertex::name)
			.subGraph(v -> {
				if (v instanceof Vertex.WithGraph) {
					Vertex.WithGraph withGraph = (Vertex.WithGraph) v;

					return Optional.of(GraphAsDot.SubGraph.of(withGraph.graph())
						.connections(withGraph.connections())
						.build());
				}
				return Optional.empty();
			})
			.build()
			.asDot(graph);

		assertThat(dotFile)
			.isEqualTo(dotFile("subgraph.dot"));
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