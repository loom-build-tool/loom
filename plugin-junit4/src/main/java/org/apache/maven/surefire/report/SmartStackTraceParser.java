package org.apache.maven.surefire.report;

import static java.util.Arrays.asList;
import static org.apache.maven.shared.utils.StringUtils.chompLast;
import static org.apache.maven.shared.utils.StringUtils.isNotEmpty;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceFilter;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class SmartStackTraceParser
{
    private static final int MAX_LINE_LENGTH = 77;

    private final SafeThrowable throwable;

    private final StackTraceElement[] stackTrace;

    private final String simpleName;

    private final String testClassName;

    private final Class testClass;

    private final String testMethodName;

    public SmartStackTraceParser( final Class testClass, final Throwable throwable )
    {
        this( testClass.getName(), throwable, null );
    }

    public SmartStackTraceParser( final String testClassName, final Throwable throwable, final String testMethodName )
    {
        this.testMethodName = testMethodName;
        this.testClassName = testClassName;
        testClass = getClass( testClassName );
        simpleName = testClassName.substring( testClassName.lastIndexOf( "." ) + 1 );
        this.throwable = new SafeThrowable( throwable );
        stackTrace = throwable.getStackTrace();
    }

    private static Class getClass( final String name )
    {
        try
        {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return classLoader != null ? classLoader.loadClass( name ) : null;
        }
        catch ( final ClassNotFoundException e )
        {
            return null;
        }
    }

    private static String getSimpleName( final String className )
    {
        final int i = className.lastIndexOf( "." );
        return className.substring( i + 1 );
    }

    @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
    public String getString()
    {
        if ( testClass == null )
        {
            return throwable.getLocalizedMessage();
        }

        final StringBuilder result = new StringBuilder();
        final List<StackTraceElement> stackTraceElements = focusOnClass( stackTrace, testClass );
        Collections.reverse( stackTraceElements );
        if ( stackTraceElements.isEmpty() )
        {
            result.append( simpleName );
            if ( isNotEmpty( testMethodName ) )
            {
                result.append( "." )
                    .append( testMethodName );
            }
        }
        else
        {
            for ( int i = 0; i < stackTraceElements.size(); i++ )
            {
                final StackTraceElement stackTraceElement = stackTraceElements.get( i );
                if ( i == 0 )
                {
                    result.append( simpleName );
                    if ( !stackTraceElement.getClassName().equals( testClassName ) )
                    {
                        result.append( ">" );
                    }
                    else
                    {
                        result.append( "." );
                    }
                }
                if ( !stackTraceElement.getClassName().equals( testClassName ) )
                {
                    result.append( getSimpleName( stackTraceElement.getClassName() ) ) // Add the name of the superclas
                        .append( "." );
                }
                result.append( stackTraceElement.getMethodName() )
                    .append( ":" )
                    .append( stackTraceElement.getLineNumber() )
                    .append( "->" );
            }

            if ( result.length() >= 2 )
            {
                result.deleteCharAt( result.length() - 1 )
                    .deleteCharAt( result.length() - 1 );
            }
        }

        final Throwable target = throwable.getTarget();
        final String exception = target.getClass().getName();
        if ( target instanceof AssertionError
            || "junit.framework.AssertionFailedError".equals( exception )
            || "junit.framework.ComparisonFailure".equals( exception ) )
        {
            final String msg = throwable.getMessage();
            if ( isNotEmpty( msg ) )
            {
                result.append( " " )
                    .append( msg );
            }
        }
        else
        {
            result.append( rootIsInclass() ? " " : " » " );
            result.append( getMinimalThrowableMiniMessage( target ) );
            result.append( getTruncatedMessage( MAX_LINE_LENGTH - result.length() ) );
        }
        return result.toString();
    }

    private static String getMinimalThrowableMiniMessage( final Throwable throwable )
    {
        final String name = throwable.getClass().getSimpleName();
        if ( name.endsWith( "Exception" ) )
        {
            return chompLast( name, "Exception" );
        }
        if ( name.endsWith( "Error" ) )
        {
            return chompLast( name, "Error" );
        }
        return name;
    }

    private String getTruncatedMessage( final int i )
    {
        if ( i < 0 )
        {
            return "";
        }
        final String msg = throwable.getMessage();
        if ( msg == null )
        {
            return "";
        }
        final String substring = msg.substring( 0, Math.min( i, msg.length() ) );
        if ( i < msg.length() )
        {
            return " " + substring + "...";
        }
        else
        {
            return " " + substring;
        }
    }

    private boolean rootIsInclass()
    {
        return stackTrace.length > 0 && stackTrace[0].getClassName().equals( testClassName );
    }

    static List<StackTraceElement> focusOnClass( final StackTraceElement[] stackTrace, final Class clazz )
    {
        final List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for ( final StackTraceElement element : stackTrace )
        {
            if ( element != null && isInSupers( clazz, element.getClassName() ) )
            {
                result.add( element );
            }
        }
        return result;
    }

    private static boolean isInSupers( Class testClass, final String lookFor )
    {
        if ( lookFor.startsWith( "junit.framework." ) )
        {
            return false;
        }
        while ( !testClass.getName().equals( lookFor ) && testClass.getSuperclass() != null )
        {
            testClass = testClass.getSuperclass();
        }
        return testClass.getName().equals( lookFor );
    }

    static Throwable findTopmostWithClass( final Throwable t, final StackTraceFilter filter )
    {
        Throwable n = t;
        do
        {
            if ( containsClassName( n.getStackTrace(), filter ) )
            {
                return n;
            }

            n = n.getCause();
        }
        while ( n != null );
        return t;
    }

    public static String stackTraceWithFocusOnClassAsString( final Throwable t, final String className )
    {
        final StackTraceFilter filter = new ClassNameStackTraceFilter( className );
        final Throwable topmost = findTopmostWithClass( t, filter );
        final List<StackTraceElement> stackTraceElements = focusInsideClass( topmost.getStackTrace(), filter );
        final String s = causeToString( topmost.getCause(), filter );
        return toString( t, stackTraceElements, filter ) + s;
    }

    static List<StackTraceElement> focusInsideClass( final StackTraceElement[] stackTrace, final StackTraceFilter filter )
    {
        final List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for ( final StackTraceElement element : stackTrace )
        {
            if ( filter.matches( element ) )
            {
                result.add( element );
            }
        }
        return result;
    }

    static boolean containsClassName( final StackTraceElement[] stackTrace, final StackTraceFilter filter )
    {
        for ( final StackTraceElement element : stackTrace )
        {
            if ( filter.matches( element ) )
            {
                return true;
            }
        }
        return false;
    }

    private static String causeToString( Throwable cause, final StackTraceFilter filter )
    {
        final StringBuilder resp = new StringBuilder();
        while ( cause != null )
        {
            resp.append( "Caused by: " );
            resp.append( toString( cause, asList( cause.getStackTrace() ), filter ) );
            cause = cause.getCause();
        }
        return resp.toString();
    }

    private static String toString( final Throwable t, final Iterable<StackTraceElement> elements, final StackTraceFilter filter )
    {
        final StringBuilder result = new StringBuilder();
        if ( t != null )
        {
            result.append( t.getClass().getName() );
            final String msg = t.getMessage();
            if ( msg != null )
            {
                result.append( ": " );
                if ( isMultiLine( msg ) )
                {
                    // SUREFIRE-986
                    result.append( '\n' );
                }
                result.append( msg );
            }
            result.append( '\n' );
        }

        for ( final StackTraceElement element : elements )
        {
            if ( filter.matches( element ) )
            {
                result.append( "\tat " )
                        .append( element )
                        .append( '\n' );
            }
        }
        return result.toString();
    }

    private static boolean isMultiLine( final String msg )
    {
        int countNewLines = 0;
        for ( int i = 0, length = msg.length(); i < length; i++ )
        {
            if ( msg.charAt( i ) == '\n' )
            {
                if ( ++countNewLines == 2 )
                {
                    break;
                }
            }
        }
        return countNewLines > 1 || countNewLines == 1 && !msg.trim().endsWith( "\n" );
    }
}
