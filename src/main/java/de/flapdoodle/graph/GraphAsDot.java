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
