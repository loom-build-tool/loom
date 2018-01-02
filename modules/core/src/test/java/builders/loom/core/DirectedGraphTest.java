/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import builders.loom.core.misc.DirectedGraph;

public class DirectedGraphTest {

    @Test
    public void simple() {
        final DirectedGraph<String> graph = new DirectedGraph<>();
        graph.addNode("foo");
        graph.addNode("bar");
        graph.addNode("baz");
        graph.addNode("bob");
        graph.addNode("alice");

        graph.addEdge("foo", "bar");
        graph.addEdge("bar", "baz");
        graph.addEdge("bob", "alice");

        assertEquals(Arrays.asList("baz", "bar", "foo"), graph.resolve("foo"));
    }

    @Test
    public void cycling() {
        final DirectedGraph<String> graph = new DirectedGraph<>();
        graph.addNode("foo");
        graph.addNode("bar");

        graph.addEdge("foo", "bar");
        graph.addEdge("bar", "foo");

        assertThrows(IllegalStateException.class, () -> graph.resolve("foo"));
    }

}
