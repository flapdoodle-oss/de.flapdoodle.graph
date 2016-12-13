package de.flapdoodle.graph;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Lazy;

@Value.Immutable
public interface Loop<V,E> {

	Set<Edge<V, E>> edges();
	
	@Auxiliary
	@Lazy
	default Set<V> vertexSet() {
		return edges().stream()
				.flatMap(e -> Stream.of(e.start(), e.end()))
				.collect(Collectors.toSet());
	}
}
