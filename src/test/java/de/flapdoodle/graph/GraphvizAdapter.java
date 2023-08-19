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
package de.flapdoodle.graph;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class GraphvizAdapter {

	public static byte[] asSvg(String dot) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Graphviz.fromString(dot)
				.render(Format.SVG_STANDALONE)
				.toOutputStream(os);
			return os.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException("could not render dot file: "+dot);
		}
	}
}
