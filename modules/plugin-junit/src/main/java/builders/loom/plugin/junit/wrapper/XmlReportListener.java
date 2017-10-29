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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

class XmlReportListener implements TestExecutionListener {

    private static final Logger LOG = Logger.getLogger(XmlReportListener.class.getName());

    private final Map<TestIdentifier, TestData> testData = new ConcurrentHashMap<>();
    private final Path reportDir;

    XmlReportListener(final Path reportDir) {
        this.reportDir = reportDir;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        testData.put(testIdentifier, TestData.start(Instant.now()));
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {

        final Throwable throwable = testExecutionResult.getThrowable().orElse(null);

        testData.get(testIdentifier).testFinished(Instant.now(),
            mapStatus(testExecutionResult.getStatus(), throwable),
            throwable);

        writeReport(testIdentifier);
    }

    @SuppressWarnings("checkstyle:returncount")
    private static TestStatus mapStatus(final TestExecutionResult.Status status,
                                        final Throwable throwable) {
        switch (status) {
            case SUCCESSFUL:
                return TestStatus.SUCCESS;
            case ABORTED:
                return TestStatus.ABORTED;
            case FAILED:
                return throwable instanceof AssertionError
                    ? TestStatus.FAILED
                    : TestStatus.ERROR;
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier,
                                 final String reason) {
        testData.put(testIdentifier, TestData.skip(reason));
        writeReport(testIdentifier);
    }

    @Override
    public void reportingEntryPublished(final TestIdentifier testIdentifier,
                                        final ReportEntry entry) {
        testData.get(testIdentifier).addReportEntry(entry);
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    private void writeReport(final TestIdentifier testIdentifier) {
        if (!isTestSuite(testIdentifier)) {
            return;
        }

        try {
            new XmlReport(buildSuite(testIdentifier), reportDir).writeReport();
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Error writing XmlReport for "
                + testIdentifier.getUniqueId(), e);
        }
    }

    // [engine:junit-jupiter]/[class:builders.loom.example.test.ExampleTest]/[method:test()]
    // [engine:junit-vintage]/[runner:x.y.z.XTest]/[test:initializationError(x.y.z.XTest)]
    private boolean isTestSuite(final TestIdentifier testIdentifier) {
        final UniqueId uniqueId = UniqueId.parse(testIdentifier.getUniqueId());

        if (!uniqueId.getEngineId().isPresent()) {
            throw new IllegalStateException("Test " + testIdentifier + " has no engine defined");
        }

        final List<UniqueId.Segment> segments = uniqueId.getSegments();

        return segments.size() == 2;
    }

    private TestSuite buildSuite(final TestIdentifier testIdentifier) {
        final TestData td = testData.get(testIdentifier);

        // virtual TestCase for initialization errors (like @BeforeAll on non-static method)
        final TestCase testClassCase = td.getStatus() != TestStatus.ERROR ? null
            : new TestCase("Test initialization", testIdentifier.getLegacyReportingName(),
            td.getDuration(), td.getStatus(), td.getThrowable(), td.getSkipReason(),
            td.getReportEntries());

        return new TestSuite(testIdentifier.getLegacyReportingName(), td.getDuration(),
            testClassCase, findTestsOfContainer(testIdentifier));
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
        final TestData td = testData.get(testIdentifier);

        final String className = findParent(testIdentifier)
            .map(TestIdentifier::getLegacyReportingName)
            .orElse(null);

        return new TestCase(testIdentifier.getDisplayName(), className, td.getDuration(),
            td.getStatus(), td.getThrowable(), td.getSkipReason(), td.getReportEntries());
    }

    private Optional<TestIdentifier> findParent(final TestIdentifier child) {
        return child.getParentId()
            .flatMap(parentId -> testData.keySet().stream()
                .filter(id -> id.getUniqueId().equals(parentId)).findFirst());
    }

}
