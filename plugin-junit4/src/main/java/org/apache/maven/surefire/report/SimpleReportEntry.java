package org.apache.maven.surefire.report;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kristian Rosenvold
 */
public class SimpleReportEntry
    implements TestSetReportEntry
{
    private final Map<String, String> systemProperties;

    private final String source;

    private final String name;

    private final StackTraceWriter stackTraceWriter;

    private final Integer elapsed;

    private final String message;

    public SimpleReportEntry()
    {
        this( null, null );
    }

    public SimpleReportEntry( final String source, final String name )
    {
        this( source, name, null, null );
    }

    public SimpleReportEntry( final String source, final String name, final Map<String, String> systemProperties )
    {
        this( source, name, null, null, systemProperties );
    }

    private SimpleReportEntry( final String source, final String name, final StackTraceWriter stackTraceWriter )
    {
        this( source, name, stackTraceWriter, null );
    }

    public SimpleReportEntry( final String source, final String name, final Integer elapsed )
    {
        this( source, name, null, elapsed );
    }

    public SimpleReportEntry( final String source, final String name, final String message )
    {
        this( source, name, null, null, message, Collections.<String, String>emptyMap() );
    }

    protected SimpleReportEntry( String source, String name, final StackTraceWriter stackTraceWriter, final Integer elapsed,
                                 final String message, final Map<String, String> systemProperties )
    {
        if ( source == null )
        {
            source = "null";
        }
        if ( name == null )
        {
            name = "null";
        }

        this.source = source;

        this.name = name;

        this.stackTraceWriter = stackTraceWriter;

        this.message = message;

        this.elapsed = elapsed;

        this.systemProperties = Collections.unmodifiableMap(new HashMap<String, String>( systemProperties ));
    }

    public SimpleReportEntry( final String source, final String name, final StackTraceWriter stackTraceWriter, final Integer elapsed )
    {
        this( source, name, stackTraceWriter, elapsed, Collections.<String, String>emptyMap() );
    }

    public SimpleReportEntry( final String source, final String name, final StackTraceWriter stackTraceWriter, final Integer elapsed,
                              final Map<String, String> systemProperties )
    {
        this( source, name, stackTraceWriter, elapsed, safeGetMessage( stackTraceWriter ), systemProperties );
    }

    public static SimpleReportEntry assumption( final String source, final String name, final String message )
    {
        return new SimpleReportEntry( source, name, message );
    }

    public static SimpleReportEntry ignored( final String source, final String name, final String message )
    {
        return new SimpleReportEntry( source, name, message );
    }

    public static SimpleReportEntry withException( final String source, final String name, final StackTraceWriter stackTraceWriter )
    {
        return new SimpleReportEntry( source, name, stackTraceWriter );
    }

    private static String safeGetMessage( final StackTraceWriter stackTraceWriter )
    {
        try
        {
            final SafeThrowable t = stackTraceWriter == null ? null : stackTraceWriter.getThrowable();
            return t == null ? null : t.getMessage();
        }
        catch ( final Throwable t )
        {
            return t.getMessage();
        }
    }

    @Override
    public String getSourceName()
    {
        return source;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getGroup()
    {
        return null;
    }

    @Override
    public StackTraceWriter getStackTraceWriter()
    {
        return stackTraceWriter;
    }

    @Override
    public Integer getElapsed()
    {
        return elapsed;
    }

    @Override
    public String toString()
    {
        return "ReportEntry{" + "source='" + source + '\'' + ", name='" + name + '\'' + ", stackTraceWriter="
            + stackTraceWriter + ", elapsed=" + elapsed + ",message=" + message + '}';
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final SimpleReportEntry that = (SimpleReportEntry) o;
        return isElapsedTimeEqual( that ) && isNameEqual( that ) && isSourceEqual( that ) && isStackEqual( that );
    }

    @Override
    public int hashCode()
    {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + ( name != null ? name.hashCode() : 0 );
        result = 31 * result + ( stackTraceWriter != null ? stackTraceWriter.hashCode() : 0 );
        result = 31 * result + ( elapsed != null ? elapsed.hashCode() : 0 );
        return result;
    }

    @Override
    public String getNameWithGroup()
    {
        return getName();
    }

    @Override
    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }

    private boolean isElapsedTimeEqual( final SimpleReportEntry en )
    {
        return elapsed != null ? elapsed.equals( en.elapsed ) : en.elapsed == null;
    }

    private boolean isNameEqual( final SimpleReportEntry en )
    {
        return name != null ? name.equals( en.name ) : en.name == null;
    }

    private boolean isSourceEqual( final SimpleReportEntry en )
    {
        return source != null ? source.equals( en.source ) : en.source == null;
    }

    private boolean isStackEqual( final SimpleReportEntry en )
    {
        return stackTraceWriter != null ? stackTraceWriter.equals( en.stackTraceWriter ) : en.stackTraceWriter == null;
    }
}
