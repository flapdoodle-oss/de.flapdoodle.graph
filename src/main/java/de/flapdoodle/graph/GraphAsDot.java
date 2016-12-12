/**
 * Copyright (C) 2013
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

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Default;
import org.jgrapht.Graph;

@Value.Immutable
public interface GraphAsDot<T> {

	@Parameter
	Function<T, String> nodeAsString();
	
	@Default
	default BiFunction<T, T, Map<String, String>> edgeAttributes() {
		return (a,b) -> Collections.emptyMap();
	}

	@Default
	default Function<T, Map<String, String>> nodeAttributes() {
		return (a) -> Collections.emptyMap();
	}

	@Default
	default String label() {
		return "graph";
	}
	
	@Auxiliary
	default <E> String asDot(Graph<T,E> graph) {
		StringBuilder sb=new StringBuilder();
		
		sb.append("digraph ").append(label()).append(" {\n")
			.append("	rankdir=LR;\n")
			.append("\n");

		graph.vertexSet().forEach(v -> {
			sb.append("\t").append(quote(nodeAsString().apply(v))).append(asNodeAttributes(nodeAttributes().apply(v))).append(";\n");
		});
		
		sb.append("\n");
		
		graph.edgeSet().forEach((edge) -> {
			T a = graph.getEdgeSource(edge);
			T b = graph.getEdgeTarget(edge);
			sb.append("\t")
				.append(quote(nodeAsString().apply(a)))
				.append(" -> ")
				.append(quote(nodeAsString().apply(b)))
				.append(asNodeAttributes(edgeAttributes().apply(a,b))).append(";\n");
		});
		
		sb.append("}\n");
		return sb.toString();
	}
	
	@Auxiliary
	default String asNodeAttributes(Map<String, String> map) {
		return map.isEmpty() 
				? "" 
				: "[ "+map.entrySet().stream().map(e -> e.getKey()+"="+quote(e.getValue())).collect(Collectors.joining(", ")) +" ]";
	}

	@Auxiliary
	default String quote(String src) {
		return "\""+src+"\"";
	}

	public static <T> ImmutableGraphAsDot.Builder<T> builder(Function<T, String> nodeAsString) {
		return ImmutableGraphAsDot.builder(nodeAsString);
	}
}
