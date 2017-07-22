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

package builders.loom.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectedGraph<T> {

    private final Map<T, Vertex<T>> vertices = new HashMap<>();

    public Set<T> nodes() {
        return Collections.unmodifiableSet(vertices.keySet());
    }

    public void addNode(final T node) {
        if (vertices.putIfAbsent(node, new Vertex<>(node)) != null) {
            throw new IllegalArgumentException("Node <" + node + "> already added");
        }
    }

    public void addEdge(final T start, final T dest) {
        final Vertex<T> startVertex = vertices.get(start);
        if (startVertex == null) {
            throw new IllegalArgumentException("Start <" + start + "> unknown");
        }

        final Vertex<T> destVertex = vertices.get(dest);
        if (destVertex == null) {
            throw new IllegalArgumentException("Dest <" + dest + "> unknown");
        }

        startVertex.addOutgoing(destVertex);
    }

    public List<T> resolve(final T dest) {
        return resolve(Collections.singletonList(dest));
    }

    public List<T> resolve(final List<T> destinations) {
        checkCycle();

        final Set<Vertex<T>> collect = new LinkedHashSet<>();
        for (final T dest : destinations) {
            final Vertex<T> destVertex = vertices.get(dest);
            if (destVertex == null) {
                throw new IllegalArgumentException("Unknown vertex <" + dest + ">");
            }
            doCollect(collect, destVertex.getOutgoing());
            collect.add(destVertex);
        }
        return collect.stream().map(Vertex::getValue).collect(Collectors.toList());
    }

    private void doCollect(final Set<Vertex<T>> collect, final List<Vertex<T>> destinations) {
        for (final Vertex<T> dest : destinations) {
            doCollect(collect, dest.getOutgoing());
        }
        collect.addAll(destinations);
    }

    private void checkCycle() {
        vertices.values().forEach(v -> checkCycle(v, new LinkedList<>()));
    }

    private void checkCycle(final Vertex<T> vertex, final LinkedList<Vertex<T>> visited) {
        if (visited.contains(vertex)) {
            throw new IllegalStateException("Graph has cyclic dependencies: <" + vertex
                + "> already seen");
        }

        visited.add(vertex);
        vertex.getOutgoing().forEach(n -> checkCycle(n, visited));
        visited.removeLast();
    }

    static final class Vertex<T> {

        private final T value;
        private final List<Vertex<T>> outgoing = new ArrayList<>();

        Vertex(final T value) {
            this.value = value;
        }

        T getValue() {
            return value;
        }

        void addOutgoing(final Vertex<T> additionalOutgoing) {
            outgoing.add(additionalOutgoing);
        }

        List<Vertex<T>> getOutgoing() {
            return outgoing;
        }

        @Override
        public String toString() {
            return value.toString();
        }

    }

}
