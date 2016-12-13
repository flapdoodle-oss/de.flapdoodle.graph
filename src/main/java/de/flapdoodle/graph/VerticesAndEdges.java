package de.flapdoodle.graph;

import java.util.Collections;
import java.util.Set;

import org.immutables.value.Value;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;

@Value.Immutable
public interface VerticesAndEdges<V, E> {

	Set<V> vertices();
	
	@Default
	default Set<Edge<V, E>> edges() {
		return Collections.emptySet();
	}
	
	@Default
	default Set<Loop<V, E>> loops() {
		return Collections.emptySet();
	}
	
	@Check
	default void check() {
		if (!edges().isEmpty() && !loops().isEmpty()) {
			throw new IllegalArgumentException("should not contain edges and loops: "+edges()+", "+loops());
		}
	}
}
