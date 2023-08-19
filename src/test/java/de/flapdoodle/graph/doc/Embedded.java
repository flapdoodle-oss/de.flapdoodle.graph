/*
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
package de.flapdoodle.graph.doc;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.ImmutableSubGraph;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Map;

@Value.Immutable
public interface Embedded extends Base {
	Map<Base, Base> connections();
	Graph<Base, DefaultEdge> graph();

	@Value.Auxiliary
	default GraphAsDot.SubGraph<Base> subGraph() {
		return ImmutableSubGraph.builder(graph())
			.putAllConnections(connections())
			.build();
	}

	static ImmutableEmbedded.Builder builder() {
		return ImmutableEmbedded.builder();
	}
}
