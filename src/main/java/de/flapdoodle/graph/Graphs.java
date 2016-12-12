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

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class Graphs {

	public static <T> Builder<T,DefaultEdge,DefaultDirectedGraph<T,DefaultEdge>> directedGraph() {
		return new Builder<>(new DefaultDirectedGraph<>(DefaultEdge.class));
	}
	
	public static class Builder<V,E,T extends Graph<V, E>> {
		
		private final T graph;

		public Builder(T graph) {
			this.graph = graph;
		}

		
		
	}
}
