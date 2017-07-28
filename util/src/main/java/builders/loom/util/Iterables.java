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

import java.util.Collection;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Iterables {

    private Iterables() {
    }

    public static <T> T getOnlyElement(final Collection<T> collection) {
        if (collection.size() == 1) {
            return collection.iterator().next();
        }

        throw new IllegalArgumentException("Expected one element, but got " + collection.size());
    }

    public static <T> T getOnlyElement(final Collection<T> collection, final T defaultValue) {
        return !collection.isEmpty() ? getOnlyElement(collection) : defaultValue;
    }

    public static Element getOnlyElement(final NodeList nodes) {
        if (nodes.getLength() == 1) {
            return (Element) nodes.item(0);
        }
        throw new IllegalArgumentException("Expected one element, but got " + nodes.getLength());
    }
}
