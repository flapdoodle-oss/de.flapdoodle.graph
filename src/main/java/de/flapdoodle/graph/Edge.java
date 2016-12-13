package de.flapdoodle.graph;

import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;

@Value.Immutable
public interface Edge<V, E> {
	@Parameter
	V start();
	@Parameter
	V end();
	@Parameter
	E edge();
}