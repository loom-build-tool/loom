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

package builders.loom.plugin.junit4.wrapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.plugin.surefire.report.LoomRunListener;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.common.junit4.JUnit4Reflector;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.util.TestSetFailedException;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import builders.loom.plugin.junit4.shared.TestResult;
import builders.loom.plugin.junit4.util.JUnit4RunListener;
import builders.loom.plugin.junit4.xml.pull.ReportEntryType;
import builders.loom.plugin.junit4.xml.pull.TestMethodStats;

/**
 * Wrapper gets injected into final classloader.
 */
public class JUnit4Wrapper {
		
	// TODO
	int skipAfterFailureCount = 10;
	
    private static final String XSD =
            "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd";

    public TestResult run(final ClassLoader classLoader, final List<Class<?>> testClasses) {

        Thread.currentThread().setContextClassLoader(classLoader);
        
        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new RunListener() {
            @Override
            public void testFailure(final Failure failure) throws Exception {
                log(failure.toString());
            }

        });
        final Result run = jUnitCore.run(computer, testClasses.toArray(new Class[]{}));


        return new TestResult(
            run.wasSuccessful(), run.getRunCount(), run.getFailureCount(), run.getIgnoreCount());

    }

	public void mavenTestRun(final List<Class<?>> testClasses) {
//		        final StatelessXmlReporter simpleXMLReporter = new StatelessXmlReporter(
//		        		reportsDirectory, reportNameSuffix, trimStackTrace, rerunFailingTestsCount, testClassMethodRunHistoryMap, xsdSchemaLocation)
				// see Junit4Provider (maven)
		
		
		final File reportDir;
		try {
			reportDir = Files.createTempDirectory("report").toFile();
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		final StatelessXmlReporter fooReporter =
	            new StatelessXmlReporter( reportDir, null, false, 0,
	                                      new ConcurrentHashMap<String, Map<String, List<WrappedReportEntry>>>(), XSD );
		        
		        
		        final LoomRunListener reporter = new LoomRunListener(fooReporter, false, true);
		        // TODO startCapture
		        final org.apache.maven.surefire.common.junit4.Notifier notifier = new org.apache.maven.surefire.common.junit4.Notifier(new JUnit4RunListener( reporter ), skipAfterFailureCount);
		        final Result result = new Result();
		        notifier.addListener(result.createListener());
		        
		        // TODO check setTestsToRun
		        final List<Class<?>> testsToRun = testClasses;
		        
		        // TODO
		        final RunResult runResult;
		        try {
			//        notifier.fireTestRunStarted( testsToRun.allowEagerReading()
			//                ? createTestsDescription( testsToRun )
			//                : createDescription( UNDETERMINED_TESTS_DESCRIPTION ) );
			        notifier.fireTestRunStarted(createTestsDescription(testClasses));
			        
			        for ( final Class<?> testToRun : testsToRun )
			        {
			            executeTestSet( testToRun, reporter, notifier );
			        }
			        
			        runResult = mergeTestHistoryResult(reporter);
		        } finally {
		            notifier.fireTestRunFinished( result );
		            notifier.removeListeners();
		        }
		        
		        System.out.println("Report dir: " + reportDir);
		        
		        System.out.println("completed:" + runResult.getCompletedCount());
		        System.out.println("failures:" + runResult.getFailures());
		        System.out.println("errors:" + runResult.getErrors());
		        System.out.println("skipped:" + runResult.getSkipped());
		        
		        try {
		        		Files.list(reportDir.toPath()).forEach(f -> System.out.println(" -> " + f));
		        }catch(final IOException e) {
		        }
	}
    
    
    static Description createTestsDescription( final Iterable<Class<?>> classes )
    {
        // "null" string rather than null; otherwise NPE in junit:4.0
        final Description description = JUnit4Reflector.createDescription( "null" );
        for ( final Class<?> clazz : classes )
        {
            description.addChild( JUnit4Reflector.createDescription( clazz.getName() ) );
        }
        return description;
    }

    private void executeTestSet( final Class<?> clazz, final org.apache.maven.surefire.report.RunListener reporter, final Notifier notifier )
    {
        final SimpleReportEntry report = new SimpleReportEntry( getClass().getName(), clazz.getName(), org.apache.maven.surefire.util.internal.ObjectUtils.systemProps() );
        reporter.testSetStarting( report );
        try
        {
            executeWithRerun( clazz, notifier );
        }
        catch ( final Throwable e )
        {
        	// TODO remove
        	e.printStackTrace();
//            if ( isFailFast() && e instanceof StoppedByUserException )
//            {
//                final String reason = e.getClass().getName();
//                final Description skippedTest = createDescription( clazz.getName(), createIgnored( reason ) );
//                notifier.fireTestIgnored( skippedTest );
//            }
//            else
            {
                final String reportName = report.getName();
                final String reportSourceName = report.getSourceName();
                final org.apache.maven.surefire.report.PojoStackTraceWriter stackWriter = new org.apache.maven.surefire.report.PojoStackTraceWriter( reportSourceName, reportName, e );
                reporter.testError( SimpleReportEntry.withException( reportSourceName, reportName, stackWriter ) );
            }
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }
    
    private static void execute( final Class<?> testClass, final Notifier notifier, final Filter filter )
    {
        final int classModifiers = testClass.getModifiers();
//        if ( !isAbstract( classModifiers ) && !isInterface( classModifiers ) )
        {
            Request request = Request.aClass( testClass );
            if ( filter != null )
            {
                request = request.filterWith( filter );
            }
            final Runner runner = request.getRunner();
//            if ( countTestsInRunner( runner.getDescription() ) != 0 )
            {
                runner.run( notifier );
            }
        }
    }

    private void executeWithRerun( final Class<?> clazz, final Notifier notifier )
        throws TestSetFailedException
    {
        final org.apache.maven.surefire.common.junit4.JUnitTestFailureListener failureListener = new org.apache.maven.surefire.common.junit4.JUnitTestFailureListener();
        notifier.addListener( failureListener );
//        final boolean hasMethodFilter = testResolver != null && testResolver.hasMethodPatterns();

        try
        {
            try
            {
//                notifier.asFailFast( isFailFast() );
//                execute( clazz, notifier, hasMethodFilter ? createMethodFilter() : null );
                execute( clazz, notifier, null/*no filter*/ );
            }
            finally
            {
                notifier.asFailFast( false );
            }

            // Rerun failing tests if rerunFailingTestsCount is larger than 0
//            if ( isRerunFailingTests() )
//            {
//                final Notifier rerunNotifier = pureNotifier();
//                notifier.copyListenersTo( rerunNotifier );
//                for ( int i = 0; i < rerunFailingTestsCount && !failureListener.getAllFailures().isEmpty(); i++ )
//                {
//                    final Set<ClassMethod> failedTests = generateFailingTests( failureListener.getAllFailures() );
//                    failureListener.reset();
//                    if ( !failedTests.isEmpty() )
//                    {
//                        executeFailedMethod( rerunNotifier, failedTests );
//                    }
//                }
//            }
        }
        finally
        {
            notifier.removeListener( failureListener );
        }
    }
    
    private RunResult mergeTestHistoryResult(final LoomRunListener listener)
    {
//        globalStats = new RunStatistics();
        final TreeMap<String, List<TestMethodStats>> flakyTests = new TreeMap<String, List<TestMethodStats>>();
        final TreeMap<String, List<TestMethodStats>> failedTests = new TreeMap<String, List<TestMethodStats>>();
        final TreeMap<String, List<TestMethodStats>> errorTests = new TreeMap<String, List<TestMethodStats>>();

        final Map<String, List<TestMethodStats>> mergedTestHistoryResult = new HashMap<String, List<TestMethodStats>>();
        // Merge all the stats for tests from listeners
//        for ( final LoomRunListener listener : listeners )
        {
            final List<TestMethodStats> testMethodStats = listener.getTestMethodStats();
            for ( final TestMethodStats methodStats : testMethodStats )
            {
                List<TestMethodStats> currentMethodStats =
                    mergedTestHistoryResult.get( methodStats.getTestClassMethodName() );
                if ( currentMethodStats == null )
                {
                    currentMethodStats = new ArrayList<TestMethodStats>();
                    currentMethodStats.add( methodStats );
                    mergedTestHistoryResult.put( methodStats.getTestClassMethodName(), currentMethodStats );
                }
                else
                {
                    currentMethodStats.add( methodStats );
                }
            }
        }

        // Update globalStatistics by iterating through mergedTestHistoryResult
        int completedCount = 0, skipped = 0;

        for ( final Map.Entry<String, List<TestMethodStats>> entry : mergedTestHistoryResult.entrySet() )
        {
            final List<TestMethodStats> testMethodStats = entry.getValue();
            final String testClassMethodName = entry.getKey();
            completedCount++;

            final List<ReportEntryType> resultTypes = new ArrayList<ReportEntryType>();
            for ( final TestMethodStats methodStats : testMethodStats )
            {
                resultTypes.add( methodStats.getResultType() );
            }

            switch ( StatelessXmlReporter.getTestResultType( resultTypes, 10/*rerunFailingTestsCount*/ ) )
            {
                case success:
                    // If there are multiple successful runs of the same test, count all of them
                    int successCount = 0;
                    for ( final ReportEntryType type : resultTypes )
                    {
                        if ( type == ReportEntryType.SUCCESS )
                        {
                            successCount++;
                        }
                    }
                    completedCount += successCount - 1;
                    break;
                case skipped:
                    skipped++;
                    break;
                case flake: // TODO remove when no rerun supported
                    flakyTests.put( testClassMethodName, testMethodStats );
                    break;
                case failure:
                    failedTests.put( testClassMethodName, testMethodStats );
                    break;
                case error:
                    errorTests.put( testClassMethodName, testMethodStats );
                    break;
                default:
                    throw new IllegalStateException( "Get unknown test result type" );
            }
        }

        return new RunResult(completedCount, errorTests.size(), failedTests.size(), skipped);
//        globalStats.set( completedCount, errorTests.size(), failedTests.size(), skipped, flakyTests.size() );
    }
    
    @SuppressWarnings("checkstyle:regexpmultiline")
    private static void log(final String message) {
        System.err.println(message);
    }

}
