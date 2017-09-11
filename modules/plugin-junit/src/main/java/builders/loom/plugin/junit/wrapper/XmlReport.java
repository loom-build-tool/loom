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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.platform.engine.reporting.ReportEntry;

import builders.loom.plugin.junit.wrapper.util.XmlBuilder;
import builders.loom.plugin.junit.wrapper.util.XmlWriter;

class XmlReport {

    private static final double MILLIS_TO_SEC = 1_000;

    private final TestSuite testSuite;
    private final XmlBuilder.Element rootElement;
    private final Path reportFile;

    XmlReport(final TestSuite testSuite, final Path reportDir) {
        this.testSuite = testSuite;
        reportFile = reportDir.resolve("TEST-" + testSuite.getName() + ".xml");
        rootElement = XmlBuilder.root("testsuite");
    }

    void writeReport() {
        writeTestSuiteAttributes();

        writeSystemProperties();

        testSuite.getTestCases().forEach(this::writeTestCase);

        new XmlWriter().write(rootElement.getDocument(), reportFile);
    }

    private void writeTestSuiteAttributes() {
        rootElement
            .attr("name", testSuite.getName())
            .attr("tests", String.valueOf(testSuite.getTestCount()))
            .attr("skipped", String.valueOf(testSuite.getSkipCount()))
            .attr("failures", String.valueOf(testSuite.getFailureCount()))
            .attr("errors", String.valueOf(testSuite.getErrorCount()))
            .attr("time", timeOfDuration(testSuite.getDuration()));
    }

    private void writeSystemProperties() {
        final XmlBuilder.Element propertiesE = rootElement.element("properties");
        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            propertiesE.element("property")
                .attr("name", String.valueOf(entry.getKey()))
                .attr("value", String.valueOf(entry.getValue()));
        }
    }

    private void writeTestCase(final TestCase testCase) {
        final XmlBuilder.Element testcaseE = rootElement.element("testcase");

        testcaseE
            .attr("classname", testCase.getClassName())
            .attr("name", testCase.getName())
            .attr("time", timeOfDuration(testCase.getDuration()));

        switch (testCase.getStatus()) {
            case SUCCESS:
                break;
            case SKIPPED:
                writeSkipped(testcaseE, testCase.getSkipReason());
                break;
            case ABORTED:
                writeAborted(testcaseE, testCase);
                break;
            case FAILED:
                writeFailed(testcaseE.element("failure"), testCase.getThrowable());
                break;
            case ERROR:
                writeFailed(testcaseE.element("error"), testCase.getThrowable());
                break;
            default:
                throw new IllegalStateException("Unknown status: " + testCase.getStatus());
        }

        writeReportEntries(testcaseE, testCase.getReportEntries());
    }

    private void writeReportEntries(final XmlBuilder.Element parent,
                                    final List<ReportEntry> reportEntries) {

        if (reportEntries.isEmpty()) {
            return;
        }

        final XmlBuilder.Element outE = parent.element("system-out");
        final StringBuilder sb = new StringBuilder();
        for (final ReportEntry entry : reportEntries) {
            sb.append("JUnit ReportEntry ");
            sb.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(entry.getTimestamp()));
            sb.append(" ");
            sb.append(entry.getKeyValuePairs().toString());
            sb.append("\n");
        }
        outE.text(sb.toString());
    }

    private void writeSkipped(final XmlBuilder.Element testcaseE, final String skipReason) {
        final XmlBuilder.Element skippedE = testcaseE.element("skipped");
        if (skipReason != null) {
            skippedE.text(skipReason);
        }
    }

    private void writeAborted(final XmlBuilder.Element testcaseE, final TestCase testCase) {
        final String reason = completeThrowableToString(testCase.getThrowable())
            .orElse(null);
        writeSkipped(testcaseE, reason);
    }

    private void writeFailed(final XmlBuilder.Element element, final Throwable throwable) {
        if (throwable != null) {
            if (throwable.getMessage() != null) {
                element.attr("message", throwable.getMessage());
            }
            element
                .attr("type", throwable.getClass().getName())
                .text(throwableToString(throwable));
        }
    }

    private static String timeOfDuration(final Duration duration) {
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        return nf.format(duration.toMillis() / MILLIS_TO_SEC);
    }

    private static String throwableToString(final Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.toString();
    }

    private static Optional<String> completeThrowableToString(final Throwable throwable) {
        if (throwable == null) {
            return Optional.empty();
        }

        final StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            if (throwable.getMessage() != null) {
                printWriter.println(throwable.getMessage());
            }
            throwable.printStackTrace(printWriter);
        }
        return Optional.of(stringWriter.toString());
    }

}
