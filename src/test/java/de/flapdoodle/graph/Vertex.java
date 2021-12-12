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

import org.immutables.builder.Builder;
import org.immutables.value.Value;
import org.jgrapht.Graph;

import java.util.Map;

public abstract class Vertex {

	public abstract String name();

	@Value.Immutable
	public static abstract class Named extends Vertex {

		@Override
		@Value.Parameter
		public abstract String name();

		public static Named of(String name) {
			return ImmutableNamed.of(name);
		}
	}

	@Value.Immutable
	public static abstract class WithGraph extends Vertex {

		@Override
		@Builder.Parameter
		public abstract String name();

		@Builder.Parameter
		public abstract Graph<Vertex,?> graph();

		public abstract Named in();

		public abstract Named out();

		public abstract Map<Named, Named> connections();

		public static ImmutableWithGraph.Builder of(String name, Graph<Vertex,?> graph) {
			return ImmutableWithGraph.builder(name,graph);
		}
	}
}
