package org.apache.maven.plugin.surefire.report;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.TestSetReportEntry;

import builders.loom.plugin.junit4.xml.pull.ReportEntryType;

public class LoomRunListener implements RunListener, ConsoleOutputReceiver {
	
	private final TestSetStats detailsForThis;
	private final StatelessXmlReporter simpleXMLReporter;
	
    private Utf8RecodingDeferredFileOutputStream testStdOut = initDeferred( "stdout" );

    private Utf8RecodingDeferredFileOutputStream testStdErr = initDeferred( "stderr" );

    private Utf8RecodingDeferredFileOutputStream initDeferred( final String channel )
    {
        return new Utf8RecodingDeferredFileOutputStream( channel );
    }
	
	public LoomRunListener(final StatelessXmlReporter simpleXMLReporter, final boolean trimStackTrace,
            final boolean isPlainFormat) {
		this.simpleXMLReporter = simpleXMLReporter;
		detailsForThis = new TestSetStats( trimStackTrace, isPlainFormat );
	}
	 
	@Override
	public void testSetStarting(final TestSetReportEntry report) {
	}

	@Override
	public void testSetCompleted(final TestSetReportEntry report) {

               try {
				simpleXMLReporter.testSetCompleted( wrapTestSet(report), detailsForThis );
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}

//        addTestMethodStats();
        detailsForThis.reset();
        clearCapture();
	}
	
	 @Override
    public void testStarting( final ReportEntry report )
    {
        detailsForThis.testStart();
    }

    @Override
    public void testSucceeded( final ReportEntry reportEntry )
    {
        final WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.SUCCESS );
        detailsForThis.testSucceeded( wrapped );
//        statisticsReporter.testSucceeded( reportEntry );
        clearCapture();
    }

    @Override
    public void testError( final ReportEntry reportEntry )
    {
        final WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.ERROR );
        detailsForThis.testError( wrapped );
//        statisticsReporter.testError( reportEntry );
        clearCapture();
    }

    @Override
    public void testFailed( final ReportEntry reportEntry )
    {
        final WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.FAILURE );
        detailsForThis.testFailure( wrapped );
//        statisticsReporter.testFailed( reportEntry );
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    @Override
    public void testSkipped( final ReportEntry reportEntry )
    {
        final WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.SKIPPED );

        detailsForThis.testSkipped( wrapped );
//        statisticsReporter.testSkipped( reportEntry );
        clearCapture();
    }

    @Override
    public void testExecutionSkippedByUser()
    {
    }

    @Override
    public void testAssumptionFailure( final ReportEntry report )
    {
        testSkipped( report );
    }

    public void clearCapture()
    {
        testStdOut = initDeferred( "stdout" );
        testStdErr = initDeferred( "stderr" );
    }
    
    private WrappedReportEntry wrap( final ReportEntry other, final ReportEntryType reportEntryType )
    {
        final int estimatedElapsed;
        if ( reportEntryType != ReportEntryType.SKIPPED )
        {
            if ( other.getElapsed() != null )
            {
                estimatedElapsed = other.getElapsed();
            }
            else
            {
                estimatedElapsed = detailsForThis.getElapsedSinceLastStart();
            }
        }
        else
        {
            estimatedElapsed = 0;
        }

        return new WrappedReportEntry( other, reportEntryType, estimatedElapsed, testStdOut, testStdErr );
    }

	private WrappedReportEntry wrapTestSet( final TestSetReportEntry other )
    {
        return new WrappedReportEntry( other, null, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceTestSetStart(), testStdOut, testStdErr, other.getSystemProperties() );
    }
    
	@Override
    public void writeTestOutput( final byte[] buf, final int off, final int len, final boolean stdout )
    {
        try
        {
            if ( stdout )
            {
                testStdOut.write( buf, off, len );
            }
            else
            {
                testStdErr.write( buf, off, len );
            }
        }
        catch ( final IOException e )
        {
            throw new RuntimeException( e );
        }
//        consoleOutputReceiver.writeTestOutput( buf, off, len, stdout );
    }
}
