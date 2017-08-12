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

package builders.loom.plugin.idea;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import builders.loom.api.product.ArtifactProduct;

class OrderEntries {

    private final Map<String, OrderEntry> entryList = new HashMap<>();

    void append(final List<ArtifactProduct> artifacts, final String scope) {
        for (final ArtifactProduct artifact : artifacts) {
            final String mainJar =
                artifact.getMainArtifact().toAbsolutePath().normalize().toString();

            final OrderEntry existingEntry = entryList.get(mainJar);
            if (existingEntry != null) {
                if (isScopeUpgrade(existingEntry.getScope(), scope)) {
                    existingEntry.setScope(scope);
                }
            } else {
                final OrderEntry orderEntry =
                    new OrderEntry(artifact.getMainArtifact(), artifact.getSourceArtifact());
                orderEntry.setScope(scope);
                entryList.put(mainJar, orderEntry);
            }
        }
    }

    private boolean isScopeUpgrade(final String currentScope, final String newScope) {
        if ("COMPILE".equals(currentScope)) {
            return false;
        }
        if ("TEST".equals(currentScope)) {
            return "COMPILE".equals(newScope);
        }
        if ("PROVIDED".equals(currentScope)) {
            return "TEST".equals(newScope) || "COMPILE".equals(newScope);
        }

        throw new IllegalStateException("Unknown Scope: " + currentScope);
    }

    Collection<OrderEntry> getEntryList() {
        return entryList.values();
    }

}
