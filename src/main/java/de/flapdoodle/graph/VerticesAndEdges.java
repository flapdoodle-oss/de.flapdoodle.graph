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
