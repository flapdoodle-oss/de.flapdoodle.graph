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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
		Vertex.Named oneX = Vertex.Named.of("x");
		Vertex.Named oneZ = Vertex.Named.of("z");

		DefaultDirectedGraph<Vertex, DefaultEdge> oneSub = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(Vertex.class, DefaultEdge.class)))
			.build(builder -> {
				Vertex.Named x = oneX;
				Vertex.Named y = Vertex.Named.of("y");
				Vertex.Named z = oneZ;
				builder.addVertices(x, y, z);
				builder.addEdge(x, y);
				builder.addEdge(y, z);
			});

		Vertex.Named otherX = Vertex.Named.of("x");
		Vertex.Named otherZ = Vertex.Named.of("z");

		DefaultDirectedGraph<Vertex, DefaultEdge> otherSub = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(Vertex.class, DefaultEdge.class)))
			.build(builder -> {
				Vertex.Named x = otherX;
				Vertex.Named y = Vertex.Named.of("y");
				Vertex.Named z = otherZ;
				builder.addVertices(x, y, z);
				builder.addEdge(x, y);
				builder.addEdge(y, z);
			});

		ImmutableWithGraph oneSubGraph = Vertex.WithGraph.of("One", oneSub)
			.in(Vertex.Named.of("One.in"))
			.out(Vertex.Named.of("One.out"))
			.putConnections(Vertex.Named.of("One.in"), oneX)
			.putConnections(Vertex.Named.of("One.out"), oneZ)
			.build();

		ImmutableWithGraph otherSubGraph = Vertex.WithGraph.of("Other", otherSub)
			.in(Vertex.Named.of("Other.in"))
			.out(Vertex.Named.of("Other.out"))
			.putConnections(Vertex.Named.of("Other.in"), otherX)
			.putConnections(Vertex.Named.of("Other.out"), otherZ)
			.build();

		DefaultDirectedGraph<Vertex, DefaultEdge> graph = Graphs.with(Graphs.graphBuilder(Graphs.directedGraph(Vertex.class, DefaultEdge.class)))
			.build(builder -> {
				Vertex.Named a = Vertex.Named.of("A");
				Vertex.WithGraph one = oneSubGraph;
				Vertex.WithGraph other = otherSubGraph;
				Vertex.Named c = Vertex.Named.of("C");

				builder.addVertices(a, c);
				builder.addVertices(one.in(), one, one.out());
				builder.addVertices(other.in(), other, other.out());

				builder.addEdge(a, one.in());
				builder.addEdge(one.in(), one);
				builder.addEdge(one, one.out());
				builder.addEdge(one.out(), c);

				builder.addEdge(a, other.in());
				builder.addEdge(other.in(), other);
				builder.addEdge(other, other.out());
				builder.addEdge(other.out(), c);

				builder.addEdge(a, c);
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
			.nodeAsLabel(Vertex::name)
			//.nodeAttributes(vertex -> mapOf("label", vertex.name()))
			.sortedBy((GraphAsDot.AsComparable<Vertex, String>) Vertex::name)
			.build()
			.asDot(graph);

		assertThat(dotFile)
			.isEqualTo(dotFile("subgraph.dot"));
	}
	
	private static Map<String, String> mapOf(String k1, String v1) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		ret.put(k1,v1);
		return ret;
	}

	private Function<Vertex, String> nameAndCounter() {
		AtomicInteger counter=new AtomicInteger();
		IdentityHashMap<Vertex, Integer> identityHashCodeCounter=new IdentityHashMap<>();

		return vertex -> {
			Integer id = identityHashCodeCounter.get(vertex);
			if (id==null) {
				id=counter.getAndIncrement();
				identityHashCodeCounter.put(vertex,id);
			}
			return vertex.name()+"#"+id;
		};
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