package org.apache.maven.plugin.surefire.report;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.maven.shared.utils.Os;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.shared.utils.io.IOUtil;
import org.apache.maven.shared.utils.xml.PrettyPrintXMLWriter;
import org.apache.maven.shared.utils.xml.XMLWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.util.internal.StringUtils;

import builders.loom.plugin.junit4.xml.pull.ReportEntryType;

@SuppressWarnings({ "javadoc", "checkstyle:javadoctype" })
// CHECKSTYLE_OFF: LineLength
/**
 * XML format reporter writing to
 * <code>TEST-<i>reportName</i>[-<i>suffix</i>].xml</code> file like written and
 * read by Ant's <a href=
 * "http://ant.apache.org/manual/Tasks/junit.html"><code>&lt;junit&gt;</code></a>
 * and <a href=
 * "http://ant.apache.org/manual/Tasks/junitreport.html"><code>&lt;junitreport&gt;</code></a>
 * tasks, then supported by many tools like CI servers. <br>
 * 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;testsuite name="<i>suite name</i>" [group="<i>group</i>"] tests="<i>0</i>" failures="<i>0</i>" errors="<i>0</i>" skipped="<i>0</i>" time="<i>0,###.###</i>">
 *  &lt;properties>
 *    &lt;property name="<i>name</i>" value="<i>value</i>"/>
 *    [...]
 *  &lt;/properties>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"/>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>error</b> message="<i>message</i>" type="<i>exception class name</i>"><i>stacktrace</i>&lt;/error>
 *    &lt;system-out><i>system out content (present only if not empty)</i>&lt;/system-out>
 *    &lt;system-err><i>system err content (present only if not empty)</i>&lt;/system-err>
 *  &lt;/testcase>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>failure</b> message="<i>message</i>" type="<i>exception class name</i>"><i>stacktrace</i>&lt;/failure>
 *    &lt;system-out><i>system out content (present only if not empty)</i>&lt;/system-out>
 *    &lt;system-err><i>system err content (present only if not empty)</i>&lt;/system-err>
 *  &lt;/testcase>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>skipped</b>/>
 *  &lt;/testcase>
 *  [...]
 * </pre>
 *
 * @author Kristian Rosenvold
 * @see <a href="http://wiki.apache.org/ant/Proposals/EnhancedTestReports">Ant's
 *      format enhancement proposal</a> (not yet implemented by Ant 1.8.2)
 */
public class StatelessXmlReporter {
	private final File reportsDirectory;

	private final String reportNameSuffix;

	private final boolean trimStackTrace;

	private final int rerunFailingTestsCount;

	private final String xsdSchemaLocation;

	// Map between test class name and a map between test method name
	// and the list of runs for each test method
	private final Map<String, Map<String, List<WrappedReportEntry>>> testClassMethodRunHistoryMap;

	public StatelessXmlReporter(final File reportsDirectory, final String reportNameSuffix, final boolean trimStackTrace,
			final int rerunFailingTestsCount, final Map<String, Map<String, List<WrappedReportEntry>>> testClassMethodRunHistoryMap,
			final String xsdSchemaLocation) {
		this.reportsDirectory = reportsDirectory;
		this.reportNameSuffix = reportNameSuffix;
		this.trimStackTrace = trimStackTrace;
		this.rerunFailingTestsCount = rerunFailingTestsCount;
		this.testClassMethodRunHistoryMap = testClassMethodRunHistoryMap;
		this.xsdSchemaLocation = xsdSchemaLocation;
	}

	public void testSetCompleted(final WrappedReportEntry testSetReportEntry, final TestSetStats testSetStats) throws IOException {
		final String testClassName = testSetReportEntry.getName();

		final Map<String, List<WrappedReportEntry>> methodRunHistoryMap = getAddMethodRunHistoryMap(testClassName);

		// Update testClassMethodRunHistoryMap
		for (final WrappedReportEntry methodEntry : testSetStats.getReportEntries()) {
			getAddMethodEntryList(methodRunHistoryMap, methodEntry);
		}

		final OutputStream outputStream = getOutputStream(testSetReportEntry);
		final OutputStreamWriter fw = getWriter(outputStream);
		try {
			final XMLWriter ppw = new PrettyPrintXMLWriter(fw);
			ppw.setEncoding(StringUtils.UTF_8.name());

			createTestSuiteElement(ppw, testSetReportEntry, testSetStats, testSetReportEntry.elapsedTimeAsString());

			showProperties(ppw, testSetReportEntry.getSystemProperties());

			// Iterate through all the test methods in the test class
			for (final Entry<String, List<WrappedReportEntry>> entry : methodRunHistoryMap.entrySet()) {
				final List<WrappedReportEntry> methodEntryList = entry.getValue();
				if (methodEntryList == null) {
					throw new IllegalStateException("Get null test method run history");
				}

				if (!methodEntryList.isEmpty()) {
					if (rerunFailingTestsCount > 0) {
						final TestResultType resultType = getTestResultType(methodEntryList);
						switch (resultType) {
						case success:
							for (final WrappedReportEntry methodEntry : methodEntryList) {
								if (methodEntry.getReportEntryType() == ReportEntryType.SUCCESS) {
									startTestElement(ppw, methodEntry, reportNameSuffix,
											methodEntryList.get(0).elapsedTimeAsString());
									ppw.endElement();
								}
							}
							break;
						case error:
						case failure:
							// When rerunFailingTestsCount is set to larger than 0
							startTestElement(ppw, methodEntryList.get(0), reportNameSuffix,
									methodEntryList.get(0).elapsedTimeAsString());
							boolean firstRun = true;
							for (final WrappedReportEntry singleRunEntry : methodEntryList) {
								if (firstRun) {
									firstRun = false;
									getTestProblems(fw, ppw, singleRunEntry, trimStackTrace, outputStream,
											singleRunEntry.getReportEntryType().getXmlTag(), false);
									createOutErrElements(fw, ppw, singleRunEntry, outputStream);
								} else {
									getTestProblems(fw, ppw, singleRunEntry, trimStackTrace, outputStream,
											singleRunEntry.getReportEntryType().getRerunXmlTag(), true);
								}
							}
							ppw.endElement();
							break;
						case flake:
							String runtime = "";
							// Get the run time of the first successful run
							for (final WrappedReportEntry singleRunEntry : methodEntryList) {
								if (singleRunEntry.getReportEntryType() == ReportEntryType.SUCCESS) {
									runtime = singleRunEntry.elapsedTimeAsString();
									break;
								}
							}
							startTestElement(ppw, methodEntryList.get(0), reportNameSuffix, runtime);
							for (final WrappedReportEntry singleRunEntry : methodEntryList) {
								if (singleRunEntry.getReportEntryType() != ReportEntryType.SUCCESS) {
									getTestProblems(fw, ppw, singleRunEntry, trimStackTrace, outputStream,
											singleRunEntry.getReportEntryType().getFlakyXmlTag(), true);
								}
							}
							ppw.endElement();

							break;
						case skipped:
							startTestElement(ppw, methodEntryList.get(0), reportNameSuffix,
									methodEntryList.get(0).elapsedTimeAsString());
							getTestProblems(fw, ppw, methodEntryList.get(0), trimStackTrace, outputStream,
									methodEntryList.get(0).getReportEntryType().getXmlTag(), false);
							ppw.endElement();
							break;
						default:
							throw new IllegalStateException("Get unknown test result type");
						}
					} else {
						// rerunFailingTestsCount is smaller than 1, but for some reasons a test could
						// be run
						// for more than once
						for (final WrappedReportEntry methodEntry : methodEntryList) {
							startTestElement(ppw, methodEntry, reportNameSuffix, methodEntry.elapsedTimeAsString());
							if (methodEntry.getReportEntryType() != ReportEntryType.SUCCESS) {
								getTestProblems(fw, ppw, methodEntry, trimStackTrace, outputStream,
										methodEntry.getReportEntryType().getXmlTag(), false);
								createOutErrElements(fw, ppw, methodEntry, outputStream);
							}
							ppw.endElement();
						}
					}
				}
			}
			ppw.endElement(); // TestSuite
		} finally {
			IOUtil.close(fw);
		}
	}

	/**
	 * Clean testClassMethodRunHistoryMap
	 */
	public void cleanTestHistoryMap() {
		testClassMethodRunHistoryMap.clear();
	}

	/**
	 * Get the result of a test from a list of its runs in WrappedReportEntry
	 *
	 * @param methodEntryList
	 *            the list of runs for a given test
	 * @return the TestResultType for the given test
	 */
	private TestResultType getTestResultType(final List<WrappedReportEntry> methodEntryList) {
		final List<ReportEntryType> testResultTypeList = new ArrayList<ReportEntryType>();
		for (final WrappedReportEntry singleRunEntry : methodEntryList) {
			testResultTypeList.add(singleRunEntry.getReportEntryType());
		}

		return getTestResultType(testResultTypeList, rerunFailingTestsCount);
	}

	/**
     * Get the result of a test based on all its runs. If it has success and failures/errors, then it is a flake;
     * if it only has errors or failures, then count its result based on its first run
     *
     * @param reportEntries the list of test run report type for a given test
     * @param rerunFailingTestsCount configured rerun count for failing tests
     * @return the type of test result
     */
    // Use default visibility for testing
    static TestResultType getTestResultType( final List<ReportEntryType> reportEntries, final int rerunFailingTestsCount  )
    {
        if ( reportEntries == null || reportEntries.isEmpty() )
        {
            return TestResultType.unknown;
        }

        boolean seenSuccess = false, seenFailure = false, seenError = false;
        for ( final ReportEntryType resultType : reportEntries )
        {
            if ( resultType == ReportEntryType.SUCCESS )
            {
                seenSuccess = true;
            }
            else if ( resultType == ReportEntryType.FAILURE )
            {
                seenFailure = true;
            }
            else if ( resultType == ReportEntryType.ERROR )
            {
                seenError = true;
            }
        }

        if ( seenFailure || seenError )
        {
            if ( seenSuccess && rerunFailingTestsCount > 0 )
            {
                return TestResultType.flake;
            }
            else
            {
                if ( seenError )
                {
                    return TestResultType.error;
                }
                else
                {
                    return TestResultType.failure;
                }
            }
        }
        else if ( seenSuccess )
        {
            return TestResultType.success;
        }
        else
        {
            return TestResultType.skipped;
        }
    }

	private Map<String, List<WrappedReportEntry>> getAddMethodRunHistoryMap(final String testClassName) {
		Map<String, List<WrappedReportEntry>> methodRunHistoryMap = testClassMethodRunHistoryMap.get(testClassName);
		if (methodRunHistoryMap == null) {
			methodRunHistoryMap = Collections.synchronizedMap(new LinkedHashMap<String, List<WrappedReportEntry>>());
			testClassMethodRunHistoryMap.put(testClassName, methodRunHistoryMap);
		}
		return methodRunHistoryMap;
	}

	private OutputStream getOutputStream(final WrappedReportEntry testSetReportEntry) {
		final File reportFile = getReportFile(testSetReportEntry, reportsDirectory, reportNameSuffix);

		final File reportDir = reportFile.getParentFile();

		// noinspection ResultOfMethodCallIgnored
		reportDir.mkdirs();

		try {
			return new BufferedOutputStream(new FileOutputStream(reportFile), 16 * 1024);
		} catch (final Exception e) {
			throw new ReporterException("When writing report", e);
		}
	}

	private static OutputStreamWriter getWriter(final OutputStream fos) {
		return new OutputStreamWriter(fos, StandardCharsets.UTF_8);
	}

	private static void getAddMethodEntryList(final Map<String, List<WrappedReportEntry>> methodRunHistoryMap,
			final WrappedReportEntry methodEntry) {
		List<WrappedReportEntry> methodEntryList = methodRunHistoryMap.get(methodEntry.getName());
		if (methodEntryList == null) {
			methodEntryList = new ArrayList<WrappedReportEntry>();
			methodRunHistoryMap.put(methodEntry.getName(), methodEntryList);
		}
		methodEntryList.add(methodEntry);
	}

	private static File getReportFile(final ReportEntry report, final File reportsDirectory, final String reportNameSuffix) {
		final String reportName = "TEST-" + report.getName();
		final String customizedReportName = StringUtils.isBlank(reportNameSuffix) ? reportName : reportName + "-" + reportNameSuffix;
		return new File(reportsDirectory, stripIllegalFilenameChars(customizedReportName + ".xml"));
	}

	  public static String stripIllegalFilenameChars( final String original )
	    {
	        String result = original;
	        final String illegalChars = getOSSpecificIllegalChars();
	        for ( int i = 0; i < illegalChars.length(); i++ )
	        {
	            result = result.replace( illegalChars.charAt( i ), '_' );
	        }

	        return result;
	    }

	    private static String getOSSpecificIllegalChars()
	    {
	        return Os.isFamily(Os.FAMILY_WINDOWS) ? "\\/:*?\"<>|\0" : "/\0";
	    }
	    
	private static void startTestElement(final XMLWriter ppw, final WrappedReportEntry report, final String reportNameSuffix,
			final String timeAsString) throws IOException {
		ppw.startElement("testcase");
		ppw.addAttribute("name", report.getReportName());
		if (report.getGroup() != null) {
			ppw.addAttribute("group", report.getGroup());
		}
		if (report.getSourceName() != null) {
			if (reportNameSuffix != null && reportNameSuffix.length() > 0) {
				ppw.addAttribute("classname", report.getSourceName() + "(" + reportNameSuffix + ")");
			} else {
				ppw.addAttribute("classname", report.getSourceName());
			}
		}
		ppw.addAttribute("time", timeAsString);
	}

	private void createTestSuiteElement(final XMLWriter ppw, final WrappedReportEntry report, final TestSetStats testSetStats,
			final String timeAsString) throws IOException {
		ppw.startElement("testsuite");

		ppw.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		ppw.addAttribute("xsi:noNamespaceSchemaLocation", xsdSchemaLocation);

		ppw.addAttribute("name", report.getReportName(reportNameSuffix));

		if (report.getGroup() != null) {
			ppw.addAttribute("group", report.getGroup());
		}

		ppw.addAttribute("time", timeAsString);

		ppw.addAttribute("tests", String.valueOf(testSetStats.getCompletedCount()));

		ppw.addAttribute("errors", String.valueOf(testSetStats.getErrors()));

		ppw.addAttribute("skipped", String.valueOf(testSetStats.getSkipped()));

		ppw.addAttribute("failures", String.valueOf(testSetStats.getFailures()));
	}

	private static void getTestProblems(final OutputStreamWriter outputStreamWriter, final XMLWriter ppw, final WrappedReportEntry report,
			final boolean trimStackTrace, final OutputStream fw, final String testErrorType, final boolean createOutErrElementsInside) throws IOException {
		ppw.startElement(testErrorType);

		final String stackTrace = report.getStackTrace(trimStackTrace);

		if (report.getMessage() != null && report.getMessage().length() > 0) {
			ppw.addAttribute("message", extraEscape(report.getMessage(), true));
		}

		if (report.getStackTraceWriter() != null) {
			// noinspection ThrowableResultOfMethodCallIgnored
			final SafeThrowable t = report.getStackTraceWriter().getThrowable();
			if (t != null) {
				if (t.getMessage() != null) {
					ppw.addAttribute("type",
							(stackTrace.contains(":") ? stackTrace.substring(0, stackTrace.indexOf(":")) : stackTrace));
				} else {
					ppw.addAttribute("type", new StringTokenizer(stackTrace).nextToken());
				}
			}
		}

		if (stackTrace != null) {
			ppw.writeText(extraEscape(stackTrace, false));
		}

		if (createOutErrElementsInside) {
			createOutErrElements(outputStreamWriter, ppw, report, fw);
		}

		ppw.endElement(); // entry type
	}

	// Create system-out and system-err elements
	private static void createOutErrElements(final OutputStreamWriter outputStreamWriter, final XMLWriter ppw,
			final WrappedReportEntry report, final OutputStream fw) throws IOException {
		final EncodingOutputStream eos = new EncodingOutputStream(fw);
		addOutputStreamElement(outputStreamWriter, eos, ppw, report.getStdout(), "system-out");
		addOutputStreamElement(outputStreamWriter, eos, ppw, report.getStdErr(), "system-err");
	}


	private static void addOutputStreamElement(final OutputStreamWriter outputStreamWriter, final EncodingOutputStream eos,
			final XMLWriter xmlWriter, final Utf8RecodingDeferredFileOutputStream utf8RecodingDeferredFileOutputStream,
			final String name) throws IOException {
		if (utf8RecodingDeferredFileOutputStream != null && utf8RecodingDeferredFileOutputStream.getByteCount() > 0) {
			xmlWriter.startElement(name);

			try {
				xmlWriter.writeText(""); // Cheat sax to emit element
				outputStreamWriter.flush();
				utf8RecodingDeferredFileOutputStream.close();
				eos.getUnderlying().write(ByteConstantsHolder.CDATA_START_BYTES); // emit cdata
				utf8RecodingDeferredFileOutputStream.writeTo(eos);
				eos.getUnderlying().write(ByteConstantsHolder.CDATA_END_BYTES);
				eos.flush();
			} catch (final IOException e) {
				throw new ReporterException("When writing xml report stdout/stderr", e);
			}
			xmlWriter.endElement();
		}
	}

	/**
	 * Adds system properties to the XML report. <br>
	 *
	 * @param xmlWriter
	 *            The test suite to report to
	 * @throws IOException 
	 */
	private static void showProperties(final XMLWriter xmlWriter, final Map<String, String> systemProperties) throws IOException {
		xmlWriter.startElement("properties");
		for (final Entry<String, String> entry : systemProperties.entrySet()) {
			final String key = entry.getKey();
			String value = entry.getValue();

			if (value == null) {
				value = "null";
			}

			xmlWriter.startElement("property");

			xmlWriter.addAttribute("name", key);

			xmlWriter.addAttribute("value", extraEscape(value, true));

			xmlWriter.endElement();
		}
		xmlWriter.endElement();
	}

	/**
	 * Handle stuff that may pop up in java that is not legal in xml
	 *
	 * @param message
	 *            The string
	 * @param attribute
	 *            true if the escaped value is inside an attribute
	 * @return The escaped string
	 */
	private static String extraEscape(final String message, final boolean attribute) {
		// Someday convert to xml 1.1 which handles everything but 0 inside string
		return containsEscapesIllegalXml10(message) ? escapeXml(message, attribute) : message;
	}

	private static final class EncodingOutputStream extends FilterOutputStream {
		private int c1;

		private int c2;

		public EncodingOutputStream(final OutputStream out) {
			super(out);
		}

		public OutputStream getUnderlying() {
			return out;
		}

		private boolean isCdataEndBlock(final int c) {
			return c1 == ']' && c2 == ']' && c == '>';
		}

		@Override
		public void write(final int b) throws IOException {
			if (isCdataEndBlock(b)) {
				out.write(ByteConstantsHolder.CDATA_ESCAPE_STRING_BYTES);
			} else if (isIllegalEscape(b)) {
				// uh-oh! This character is illegal in XML 1.0!
				// http://www.w3.org/TR/1998/REC-xml-19980210#charsets
				// we're going to deliberately doubly-XML escape it...
				// there's nothing better we can do! :-(
				// SUREFIRE-456
				out.write(ByteConstantsHolder.AMP_BYTES);
				out.write(String.valueOf(b).getBytes(StandardCharsets.UTF_8));
				out.write(';'); // & Will be encoded to amp inside xml encodingSHO
			} else {
				out.write(b);
			}
			c1 = c2;
			c2 = b;
		}
	}

	private static boolean containsEscapesIllegalXml10(final String message) {
		final int size = message.length();
		for (int i = 0; i < size; i++) {
			if (isIllegalEscape(message.charAt(i))) {
				return true;
			}

		}
		return false;
	}

	private static boolean isIllegalEscape(final char c) {
		return isIllegalEscape((int) c);
	}

	private static boolean isIllegalEscape(final int c) {
		return c >= 0 && c < 32 && c != '\n' && c != '\r' && c != '\t';
	}

	private static String escapeXml(final String text, final boolean attribute) {
		final StringBuilder sb = new StringBuilder(text.length() * 2);
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (isIllegalEscape(c)) {
				// uh-oh! This character is illegal in XML 1.0!
				// http://www.w3.org/TR/1998/REC-xml-19980210#charsets
				// we're going to deliberately doubly-XML escape it...
				// there's nothing better we can do! :-(
				// SUREFIRE-456
				sb.append(attribute ? "&#" : "&amp#").append((int) c).append(';'); // & Will be encoded to amp inside
																					// xml encodingSHO
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static final class ByteConstantsHolder {
		private static final byte[] CDATA_START_BYTES;

		private static final byte[] CDATA_END_BYTES;

		private static final byte[] CDATA_ESCAPE_STRING_BYTES;

		private static final byte[] AMP_BYTES;

		static {
			CDATA_START_BYTES = "<![CDATA[".getBytes(StandardCharsets.UTF_8);
			CDATA_END_BYTES = "]]>".getBytes(StandardCharsets.UTF_8);
			CDATA_ESCAPE_STRING_BYTES = "]]><![CDATA[>".getBytes(StandardCharsets.UTF_8);
			AMP_BYTES = "&amp#".getBytes(StandardCharsets.UTF_8);
		}
	}
}
