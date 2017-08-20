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

package builders.loom.log;

import org.slf4j.Marker;
import org.slf4j.event.Level;

public class LogFilter {

    private static final String PACKAGE_PREFIX = "builders.loom.";

    boolean isEnabled(final String name, final Level level) {
        if (level.toInt() >= Level.WARN.toInt()) {
            return true;
        }

        return level.toInt() > Level.TRACE.toInt() && name.startsWith(PACKAGE_PREFIX);
    }

    public boolean isEnabled(final String name, final Level level, final Marker marker) {
        return isEnabled(name, level);
    }

}
