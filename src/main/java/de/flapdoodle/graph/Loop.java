package de.flapdoodle.graph;

import java.util.Set;

import org.immutables.value.Value;

@Value.Immutable
public interface Loop<V,E> {

	Set<Edge<V, E>> edges();
}
