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

package builders.loom.plugin.junit.wrapper;

import java.time.Duration;

class TestCase {

    private final String name;
    private final String className;
    private final Duration duration;
    private boolean failed;

    TestCase(final String name, final String className, final Duration duration) {
        this.name = name;
        this.className = className;
        this.duration = duration;
    }

    String getName() {
        return name;
    }

    String getClassName() {
        return className;
    }

    Duration getDuration() {
        return duration;
    }

    boolean isFailed() {
        return failed;
    }

    static boolean isError(final TestCase testCase) {
        return false;
    }

    static boolean isSkipped(final TestCase testCase) {
        return false;
    }

}
