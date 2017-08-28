package org.apache.maven.plugin.surefire.report;

import static org.apache.maven.surefire.util.internal.StringUtils.NL;

import java.util.Collections;
import java.util.Map;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.TestSetReportEntry;

import builders.loom.plugin.junit4.xml.pull.ReportEntryType;

/**
 * @author Kristian Rosenvold
 */
public class WrappedReportEntry
    implements TestSetReportEntry
{
    private final ReportEntry original;

    private final ReportEntryType reportEntryType;

    private final Integer elapsed;

    private final Utf8RecodingDeferredFileOutputStream stdout;

    private final Utf8RecodingDeferredFileOutputStream stdErr;

    private final Map<String, String> systemProperties;

    public WrappedReportEntry( final ReportEntry original, final ReportEntryType reportEntryType, final Integer estimatedElapsed,
                               final Utf8RecodingDeferredFileOutputStream stdout,
                               final Utf8RecodingDeferredFileOutputStream stdErr,
                               final Map<String, String> systemProperties )
    {
        this.original = original;
        this.reportEntryType = reportEntryType;
        this.elapsed = estimatedElapsed;
        this.stdout = stdout;
        this.stdErr = stdErr;
        this.systemProperties = Collections.unmodifiableMap( systemProperties );
    }

    public WrappedReportEntry( final ReportEntry original, final ReportEntryType reportEntryType, final Integer estimatedElapsed,
                               final Utf8RecodingDeferredFileOutputStream stdout,
                               final Utf8RecodingDeferredFileOutputStream stdErr )
    {
        this( original, reportEntryType, estimatedElapsed, stdout, stdErr, Collections.<String, String>emptyMap() );
    }

    @Override
    public Integer getElapsed()
    {
        return elapsed;
    }

    public ReportEntryType getReportEntryType()
    {
        return reportEntryType;
    }

    public Utf8RecodingDeferredFileOutputStream getStdout()
    {
        return stdout;
    }

    public Utf8RecodingDeferredFileOutputStream getStdErr()
    {
        return stdErr;
    }

    @Override
    public String getSourceName()
    {
        return original.getSourceName();
    }

    @Override
    public String getName()
    {
        return original.getName();
    }

    public String getClassMethodName()
    {
        return getSourceName() + "." + getName();
    }

    @Override
    public String getGroup()
    {
        return original.getGroup();
    }

    @Override
    public StackTraceWriter getStackTraceWriter()
    {
        return original.getStackTraceWriter();
    }

    @Override
    public String getMessage()
    {
        return original.getMessage();
    }

    public String getStackTrace( final boolean trimStackTrace )
    {
        final StackTraceWriter w = original.getStackTraceWriter();
        return w == null ? null : ( trimStackTrace ? w.writeTrimmedTraceToString() : w.writeTraceToString() );
    }

    public String elapsedTimeAsString()
    {
        return elapsedTimeAsString( getElapsed() );
    }

    String elapsedTimeAsString( final long runTime )
    {
        return ReporterUtils.formatElapsedTime( runTime );
    }

    public String getReportName()
    {
        final int i = getName().lastIndexOf( "(" );
        return i > 0 ? getName().substring( 0, i ) : getName();
    }

    public String getReportName( final String suffix )
    {
        return suffix != null && suffix.length() > 0 ? getReportName() + "(" + suffix + ")" : getReportName();
    }

    public String getOutput( final boolean trimStackTrace )
    {
        return getElapsedTimeSummary() + "  <<< " + getReportEntryType().toString().toUpperCase() + "!" + NL
            + getStackTrace( trimStackTrace );
    }

    public String getElapsedTimeVerbose()
    {
        return "Time elapsed: " + elapsedTimeAsString() + " s";
    }

    public String getElapsedTimeSummary()
    {
        return getName() + "  " + getElapsedTimeVerbose();
    }

    public boolean isErrorOrFailure()
    {
        final ReportEntryType thisType = getReportEntryType();
        return ReportEntryType.FAILURE == thisType || ReportEntryType.ERROR == thisType;
    }

    public boolean isSkipped()
    {
        return ReportEntryType.SKIPPED == getReportEntryType();
    }

    public boolean isSucceeded()
    {
        return ReportEntryType.SUCCESS == getReportEntryType();
    }

    @Override
    public String getNameWithGroup()
    {
        return original.getNameWithGroup();
    }

    @Override
    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }
}
