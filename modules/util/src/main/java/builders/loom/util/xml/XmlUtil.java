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

package builders.loom.util.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import builders.loom.util.Preconditions;

public final class XmlUtil {

    private XmlUtil() {
    }

    public static Element getOnlyElement(final NodeList nodes) {
        if (nodes.getLength() == 1) {
            return (Element) nodes.item(0);
        }
        throw new IllegalArgumentException("Expected one element, but got " + nodes.getLength());
    }

    public static Iterable<Node> iterable(final NodeList nodes) {
        return () ->
            new NodeListIterator(nodes);
    }

    public static Iterable<Element> iterableElements(final NodeList nodes) {

        return () ->
            new NodeListElementIterator(nodes);
    }

    private static final class NodeListElementIterator implements Iterator<Element> {

        private final Iterator<Node> it;

        private NodeListElementIterator(final NodeList nodes) {
            it = iterable(nodes).iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Element next() {
            if (hasNext()) {
                final Node node = it.next();
                Preconditions.checkState(
                    node.getNodeType() == Node.ELEMENT_NODE,
                    "Cannot cast node type " + node.getNodeType() + " to element"
                );
                return (Element) node;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NodeListIterator implements Iterator<Node> {

        private final NodeList nodes;

        private int index;

        private NodeListIterator(final NodeList nodes) {
            this.nodes = nodes;
        }

        @Override
        public boolean hasNext() {
            return index < nodes.getLength();
        }

        @Override
        public Node next() {
            if (hasNext()) {
                return nodes.item(index++);
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
