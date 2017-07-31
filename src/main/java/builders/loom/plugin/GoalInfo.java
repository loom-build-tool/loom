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

package builders.loom.plugin;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class GoalInfo {

    private final String name;
    private final Set<String> usedProducts;

    public GoalInfo(final String name, final Set<String> usedProducts) {
        this.name = name;
        this.usedProducts = Collections.unmodifiableSet(usedProducts);
    }

    public String getName() {
        return name;
    }

    public Set<String> getUsedProducts() {
        return usedProducts;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GoalInfo goalInfo = (GoalInfo) o;
        return Objects.equals(name, goalInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
