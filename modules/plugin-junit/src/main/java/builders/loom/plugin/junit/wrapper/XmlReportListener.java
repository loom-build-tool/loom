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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

class XmlReportListener implements TestExecutionListener {

    private final Map<TestIdentifier, TestData> testData = new ConcurrentHashMap<>();
    private final Path reportDir;

    XmlReportListener(final Path reportDir) {
        this.reportDir = reportDir;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        testData.put(testIdentifier, new TestData());
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {
        testData.get(testIdentifier).testFinished(
            mapStatus(testExecutionResult.getStatus()),
            testExecutionResult.getThrowable().orElse(null));

        if (isTestSuite(testIdentifier)) {
            XmlReport.writeReport(build(testIdentifier), reportDir);
        }
    }

    private static TestStatus mapStatus(final TestExecutionResult.Status status) {
        switch (status) {
            case SUCCESSFUL:
                return TestStatus.SUCCESS;
            case ABORTED:
                return TestStatus.ABORTED;
            case FAILED:
                return TestStatus.FAILED;
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier,
                                 final String reason) {
        testData.get(testIdentifier).testSkipped(reason);
    }

    private TestSuite build(final TestIdentifier testIdentifier) {
        return new TestSuite(testIdentifier.getLegacyReportingName(),
            testData.get(testIdentifier).getDuration(),
            findTestsOfContainer(testIdentifier));
    }

    // [engine:junit-jupiter]/[class:builders.loom.example.test.ExampleTest]/[method:test()]
    private boolean isTestSuite(final TestIdentifier testIdentifier) {
        final Set<String> segmentTypes = UniqueId
            .parse(testIdentifier.getUniqueId()).getSegments().stream()
            .map(UniqueId.Segment::getType)
            .collect(Collectors.toSet());

        return segmentTypes.contains("class") && !segmentTypes.contains("method");
    }

    private List<TestCase> findTestsOfContainer(final TestIdentifier testIdentifier) {
        final String uniqueId = testIdentifier.getUniqueId();

        return testData.keySet().stream()
            .filter(TestIdentifier::isTest)
            .filter(i -> uniqueId.equals(i.getParentId().orElse(null)))
            .map(this::mapTestCase)
            .collect(Collectors.toList());
    }

    private TestCase mapTestCase(final TestIdentifier testIdentifier) {
        final MethodSource methodSource = getMethodSource(testIdentifier);
        final TestData td = this.testData.get(testIdentifier);

        return new TestCase(methodSource.getMethodName(), methodSource.getClassName(),
            td.getDuration(),
            td.getStatus(),
            td.getThrowable(),
            td.getSkipReason());
    }

    private static MethodSource getMethodSource(final TestIdentifier testIdentifier) {
        final TestSource testSource = testIdentifier.getSource()
            .orElseThrow(() -> new IllegalStateException("Found no testSource of "
                + testIdentifier));

        if (!(testSource instanceof MethodSource)) {
            throw new IllegalStateException("TestSource of " + testIdentifier + " is "
                + "of class " + testSource.getClass() + "! Required is " + MethodSource.class);
        }

        return (MethodSource) testSource;
    }

}
