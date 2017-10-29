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
import java.util.stream.Stream;

class TestSuite {

    private final String name;
    private final Duration duration;
    private final TestCase testClassCase;
    private final List<TestCase> testCases;

    TestSuite(final String name, final Duration duration, final TestCase testClassCase,
              final List<TestCase> testCases) {
        this.name = name;
        this.duration = duration;
        this.testClassCase = testClassCase;
        this.testCases = testCases;
    }

    String getName() {
        return name;
    }

    Duration getDuration() {
        return duration;
    }

    TestCase getTestClassCase() {
        return testClassCase;
    }

    List<TestCase> getTestCases() {
        return testCases;
    }

    int getTestCount() {
        return (int) allTests().count();
    }

    long getFailureCount() {
        return allTests().filter(TestCase::isFailed).count();
    }

    long getErrorCount() {
        return allTests().filter(TestCase::isError).count();
    }

    long getSkipCount() {
        return allTests().filter(TestCase::isSkipped).count();
    }

    private Stream<TestCase> allTests() {
        return Stream.concat(Stream.ofNullable(testClassCase), testCases.stream());
    }

}
