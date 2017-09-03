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

public class TestCase {

    private final String name;
    private final String className;
    private final Duration duration;
    private boolean failed;

    public TestCase(final String name, final String className, final Duration duration) {
        this.name = name;
        this.className = className;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean isFailed() {
        return failed;
    }

    public static boolean isError(final TestCase testCase) {
        return false;
    }

    public static boolean isSkipped(final TestCase testCase) {
        return false;
    }

    @Override
    public String toString() {
        return "TestCase{" +
            "name='" + name + '\'' +
            ", className='" + className + '\'' +
            ", duration=" + duration +
            ", failed=" + failed +
            '}';
    }
}
