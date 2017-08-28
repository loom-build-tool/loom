package org.apache.maven.surefire.common.junit4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import builders.loom.plugin.junit4.util.JUnit4RunListener;

/**
 * Extends {@link RunNotifier JUnit notifier},
 * encapsulates several different types of {@link RunListener JUnit listeners}, and
 * fires events to listeners.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class Notifier
    extends RunNotifier
{
    private final Collection<RunListener> listeners = new ArrayList<RunListener>();

    private final Queue<String> testClassNames = new ConcurrentLinkedQueue<String>();

    public Notifier( final JUnit4RunListener reporter, final int skipAfterFailureCount )
    {
        addListener( reporter );
    }

    @Override
    @SuppressWarnings( "checkstyle:redundantthrowscheck" ) // checkstyle is wrong here, see super.fireTestStarted()
    public final void fireTestStarted( final Description description ) throws StoppedByUserException
    {
        // If fireTestStarted() throws exception (== skipped test), the class must not be removed from testClassNames.
        // Therefore this class will be removed only if test class started with some test method.
        super.fireTestStarted( description );
        if ( !testClassNames.isEmpty() )
        {
            testClassNames.remove( JUnit4ProviderUtil.cutTestClassAndMethod( description ).getClazz() );
        }
    }


    @Override
    public final void addListener( final RunListener listener )
    {
        listeners.add( listener );
        super.addListener( listener );
    }

    @Override
    public final void removeListener( final RunListener listener )
    {
        listeners.remove( listener );
        super.removeListener( listener );
    }

}
