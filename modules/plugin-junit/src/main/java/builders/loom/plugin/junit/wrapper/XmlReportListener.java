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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class XmlReportListener implements TestExecutionListener {

    private final Map<TestIdentifier, TestData> testData = new ConcurrentHashMap<>();
    private final Path reportDir;

    public XmlReportListener(final Path reportDir) {
        this.reportDir = reportDir;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        testData.put(testIdentifier, new TestData(testIdentifier.getLegacyReportingName()));
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {
        testData.get(testIdentifier).testFinished(
            testExecutionResult.getStatus(),
            testExecutionResult.getThrowable().orElse(null));

        if (isTestSuite(testIdentifier)) {
            XmlReport.writeReport(build(testIdentifier), reportDir);
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier,
                                 final String reason) {
        testData.get(testIdentifier).testSkipped(reason);
    }

    public List<TestSuite> build() {
        final List<TestSuite> testSuites = new ArrayList<>();
        for (Map.Entry<TestIdentifier, TestData> entry : testData.entrySet()) {
            final TestIdentifier testIdentifier = entry.getKey();
            final TestData testData2 = entry.getValue();
            if (isTestSuite(testIdentifier)) {
                final List<TestCase> collect = findTestsOfContainer(testIdentifier);

                testSuites.add(new TestSuite(testIdentifier.getLegacyReportingName(),
                    testData2.getDuration(), collect));
            }
        }

        return testSuites;
    }

    private TestSuite build(final TestIdentifier testIdentifier) {
        return new TestSuite(testIdentifier.getLegacyReportingName(),
            testData.get(testIdentifier).getDuration(),
            findTestsOfContainer(testIdentifier));
    }

    // [engine:junit-jupiter]/[class:builders.loom.example.test.ExampleTest]/[method:test()]
    private boolean isTestSuite(final TestIdentifier testIdentifier) {
        final List<UniqueId.Segment> segments =
            UniqueId.parse(testIdentifier.getUniqueId()).getSegments();

        return segments.stream().anyMatch(s -> s.getType().equals("class"))
            && segments.stream().noneMatch(s -> s.getType().equals("method"));
    }

    private List<TestCase> findTestsOfContainer(final TestIdentifier testIdentifier) {
        final String uniqueId = testIdentifier.getUniqueId();

        return testData.keySet().stream()
            .filter(TestIdentifier::isTest)
            .filter(i -> uniqueId.equals(i.getParentId().orElse(null)))
            .map(this::mapTestCase)
            .collect(Collectors.toList());
    }

    private TestCase mapTestCase(TestIdentifier testIdentifier) {
        final MethodSource methodSource = (MethodSource) testIdentifier.getSource().get();
        return new TestCase(methodSource.getMethodName(), methodSource.getClassName(), testData.get(testIdentifier).getDuration());
    }

}
