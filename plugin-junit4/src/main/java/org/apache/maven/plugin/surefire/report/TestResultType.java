package org.apache.maven.plugin.surefire.report;

public enum TestResultType
{

    error(   "Errors: "   ),
    failure( "Failures: " ),
    success( "Success: "  ),
    skipped( "Skipped: "  ),
    unknown( "Unknown: "  );

    private final String logPrefix;

    TestResultType( final String logPrefix )
    {
        this.logPrefix = logPrefix;
    }

    public String getLogPrefix()
    {
        return logPrefix;
    }
}
