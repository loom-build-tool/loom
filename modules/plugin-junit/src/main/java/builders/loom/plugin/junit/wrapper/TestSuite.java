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
import java.util.List;

public class TestSuite {

    private final String name;
    private final Duration duration;
    private final List<TestCase> testCases;

    public TestSuite(final String name, final Duration duration,
                     final List<TestCase> testCases) {
        this.name = name;
        this.duration = duration;
        this.testCases = testCases;
    }

    public String getName() {
        return name;
    }

    public int getTestCount() {
        return testCases.size();
    }

    public long getFailureCount() {
        return testCases.stream().filter(TestCase::isFailed).count();
    }

    public long getErrorCount() {
        return testCases.stream().filter(TestCase::isError).count();
    }

    public long getSkipCount() {
        return testCases.stream().filter(TestCase::isSkipped).count();
    }

    public Duration getDuration() {
        return duration;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    @Override
    public String toString() {
        return "TestSuite{" +
            "name='" + name + '\'' +
            ", duration=" + duration +
            ", testCases=" + testCases +
            '}';
    }
}
