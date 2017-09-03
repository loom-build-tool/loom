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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

class XmlReportListener implements TestExecutionListener {

    private static final Logger LOG = Logger.getLogger(XmlReportListener.class.getName());

    private final Map<TestIdentifier, TestData> testData = new ConcurrentHashMap<>();
    private final Path reportDir;

    XmlReportListener(final Path reportDir) {
        this.reportDir = reportDir;
    }

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        LOG.info("Started testPlan " + testPlan.getRoots());
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        LOG.info("Finished testPlan " + testPlan.getRoots());
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        testData.put(testIdentifier, new TestData(Instant.now()));
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {
        final Throwable throwable = testExecutionResult.getThrowable().orElse(null);
        testData.get(testIdentifier).testFinished(Instant.now(),
            mapStatus(testExecutionResult.getStatus(), throwable),
            throwable);

        if (isTestSuite(testIdentifier)) {
            try (XmlReport xmlReport = new XmlReport(build(testIdentifier), reportDir)) {
                xmlReport.writeReport();
            } catch (final XMLStreamException e) {
                throw new IllegalStateException(e);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
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
        testData.get(testIdentifier).testSkipped(Instant.now(), reason);
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
            throw new IllegalStateException(String.format(
                "TestSource of %s is of class %s! Required is %s",
                testIdentifier, testSource.getClass(), MethodSource.class));
        }

        return (MethodSource) testSource;
    }

}
