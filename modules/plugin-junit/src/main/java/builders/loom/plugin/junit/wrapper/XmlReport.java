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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

class XmlReport {

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final double NANO_TO_SEC = 1_000_000_000D;

    private XmlReport() {
    }

    static void writeReport(final TestSuite testSuite, final Path reportDir) {
        final Path reportFile = reportDir.resolve("TEST-" + testSuite.getName() + ".xml");
        try (Writer writer = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8)) {
            final XMLStreamWriter xmlWriter = OUTPUT_FACTORY.createXMLStreamWriter(writer);

            xmlWriter.writeStartDocument("UTF-8", "1.0");
            newLine(xmlWriter);

            xmlWriter.writeStartElement("testsuite");
            xmlWriter.writeAttribute("name", testSuite.getName());
            xmlWriter.writeAttribute("tests", String.valueOf(testSuite.getTestCount()));
            xmlWriter.writeAttribute("skipped", String.valueOf(testSuite.getSkipCount()));
            xmlWriter.writeAttribute("failures", String.valueOf(testSuite.getFailureCount()));
            xmlWriter.writeAttribute("errors", String.valueOf(testSuite.getErrorCount()));
            xmlWriter.writeAttribute("time", timeOfDuration(testSuite.getDuration()));
            newLine(xmlWriter);

            xmlWriter.writeStartElement("properties");
            newLine(xmlWriter);
            for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
                xmlWriter.writeEmptyElement("property");
                xmlWriter.writeAttribute("name", String.valueOf(entry.getKey()));
                xmlWriter.writeAttribute("value", String.valueOf(entry.getValue()));
                newLine(xmlWriter);
            }
            xmlWriter.writeEndElement();
            newLine(xmlWriter);

            for (final TestCase testCase : testSuite.getTestCases()) {
                xmlWriter.writeStartElement("testcase");
                xmlWriter.writeAttribute("classname", testCase.getClassName());
                xmlWriter.writeAttribute("name", testCase.getName());
                xmlWriter.writeAttribute("time", timeOfDuration(testCase.getDuration()));
                newLine(xmlWriter);

                switch (testCase.getStatus()) {
                    case SUCCESS:
                        break;
                    case SKIPPED:
                        xmlWriter.writeEmptyElement("skipped");
                        break;
                    case FAILED:
                        final Throwable throwable = testCase.getThrowable();
                        if (throwable != null) {
                            xmlWriter.writeStartElement(testCase.isError() ? "error" : "failure");
                            xmlWriter.writeAttribute("type", throwable.getClass().getName());
                            if (throwable.getMessage() != null) {
                                xmlWriter.writeAttribute("message", throwable.getMessage());
                            }
                            xmlWriter.writeCharacters(throwableToString(throwable));
                            xmlWriter.writeEndElement();
                        } else {
                            xmlWriter.writeEmptyElement("error");
                        }
                        newLine(xmlWriter);
                        break;
                    case ABORTED:
                        // TODO how to handle this?
                        break;
                    default:
                        throw new IllegalStateException("Unknown status: " + testCase.getStatus());
                }

                xmlWriter.writeEndElement();
                newLine(xmlWriter);
            }

            // end testsuite
            xmlWriter.writeEndElement();
            newLine(xmlWriter);

            xmlWriter.writeEndDocument();
            xmlWriter.close();
        } catch (final XMLStreamException e) {
            throw new IllegalStateException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void newLine(final XMLStreamWriter xmlWriter) throws XMLStreamException {
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

}
