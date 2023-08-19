package de.flapdoodle.graph.doc;

import org.immutables.value.Value;

@Value.Immutable
public interface Named extends Base {
	@Value.Parameter
	@Override
	String name();

	static Named of(String name) {
		return ImmutableNamed.of(name);
	}
}
