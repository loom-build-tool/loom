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

package builders.loom.plugin.spotbugs;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.Priorities;

final class SpotBugsUtil {

    private SpotBugsUtil() {
    }

    static int resolvePriority(final String prio) {
        final String ucPrio = prio.toUpperCase();

        return Stream.of(Priorities.class.getDeclaredFields())
            .filter(f -> f.getType().equals(int.class))
            .filter(f -> f.getName().equals(ucPrio + "_PRIORITY"))
            .findFirst()
            .map(f -> {
                try {
                    return f.getInt(null);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            })
            .orElseThrow(() -> new IllegalStateException("Unknown SpotBugs priority: " + prio));
    }

}
