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
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {

                return new Iterator<Node>() {

                    int index = 0;

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
                };
            }
        };
    }

}
