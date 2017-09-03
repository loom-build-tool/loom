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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

class XmlReport implements Closeable {

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final double NANO_TO_SEC = 1_000_000_000D;
    private final TestSuite testSuite;
    private final BufferedWriter writer;
    private final XMLStreamWriter xmlWriter;

    XmlReport(final TestSuite testSuite, final Path reportDir)
        throws IOException, XMLStreamException {

        this.testSuite = testSuite;
        final Path reportFile = reportDir.resolve("TEST-" + testSuite.getName() + ".xml");
        writer = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8);
        xmlWriter = OUTPUT_FACTORY.createXMLStreamWriter(writer);
    }

    void writeReport() throws XMLStreamException {
        xmlWriter.writeStartDocument("UTF-8", "1.0");
        newLine();

        xmlWriter.writeStartElement("testsuite");
        xmlWriter.writeAttribute("name", testSuite.getName());
        xmlWriter.writeAttribute("tests", String.valueOf(testSuite.getTestCount()));
        xmlWriter.writeAttribute("skipped", String.valueOf(testSuite.getSkipCount()));
        xmlWriter.writeAttribute("failures", String.valueOf(testSuite.getFailureCount()));
        xmlWriter.writeAttribute("errors", String.valueOf(testSuite.getErrorCount()));
        xmlWriter.writeAttribute("time", timeOfDuration(testSuite.getDuration()));
        newLine();

        xmlWriter.writeStartElement("properties");
        newLine();
        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            xmlWriter.writeEmptyElement("property");
            xmlWriter.writeAttribute("name", String.valueOf(entry.getKey()));
            xmlWriter.writeAttribute("value", String.valueOf(entry.getValue()));
            newLine();
        }
        xmlWriter.writeEndElement();
        newLine();

        for (final TestCase testCase : testSuite.getTestCases()) {
            writeTestCase(testCase);
        }

        // end testsuite
        xmlWriter.writeEndElement();
        newLine();

        xmlWriter.writeEndDocument();
        xmlWriter.close();
    }

    private void writeTestCase(final TestCase testCase) throws XMLStreamException {
        if (testCase.getStatus() == TestStatus.SUCCESS) {
            xmlWriter.writeEmptyElement("testcase");
            writeTestCaseAttributes(testCase);
            newLine();
            return;
        }

        xmlWriter.writeStartElement("testcase");
        writeTestCaseAttributes(testCase);
        newLine();

        switch (testCase.getStatus()) {
            case SKIPPED:
                writeSkipped(testCase.getSkipReason());
                break;
            case ABORTED:
                writeAborted(testCase);
                break;
            case FAILED:
                writeFailed(testCase.getThrowable(), "failure");
                break;
            case ERROR:
                writeFailed(testCase.getThrowable(), "error");
                break;
            default:
                throw new IllegalStateException("Unknown status: " + testCase.getStatus());
        }

        // end testcase
        xmlWriter.writeEndElement();
        newLine();
    }

    private void writeTestCaseAttributes(final TestCase testCase) throws XMLStreamException {
        xmlWriter.writeAttribute("classname", testCase.getClassName());
        xmlWriter.writeAttribute("name", testCase.getName());
        xmlWriter.writeAttribute("time", timeOfDuration(testCase.getDuration()));
    }

    private void writeSkipped(final String skipReason) throws XMLStreamException {
        if (skipReason == null) {
            xmlWriter.writeEmptyElement("skipped");
        } else {
            xmlWriter.writeStartElement("skipped");
            xmlWriter.writeCharacters(skipReason);
            xmlWriter.writeEndElement();
        }

        newLine();
    }

    private void writeAborted(final TestCase testCase) throws XMLStreamException {
        final String reason = completeThrowableToString(testCase.getThrowable())
            .orElse(null);
        writeSkipped(reason);
    }

    private void writeFailed(final Throwable throwable, final String errorType)
        throws XMLStreamException {

        if (throwable == null) {
            xmlWriter.writeEmptyElement(errorType);
        } else {
            xmlWriter.writeStartElement(errorType);
            xmlWriter.writeAttribute("type", throwable.getClass().getName());
            if (throwable.getMessage() != null) {
                xmlWriter.writeAttribute("message", throwable.getMessage());
            }
            xmlWriter.writeCharacters(throwableToString(throwable));
            xmlWriter.writeEndElement();
        }
        newLine();
    }

    private void newLine() throws XMLStreamException {
        xmlWriter.writeCharacters("\n");
    }

    private static String timeOfDuration(final Duration duration) {
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        return nf.format(duration.getNano() / NANO_TO_SEC);
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

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
