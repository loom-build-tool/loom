package org.apache.maven.plugin.surefire.report;

public interface ConsoleOutputReceiver
{

    /**
     * Forwards process output from the running test-case into the reporting system
     *
     * @param buf    the buffer to write
     * @param off    offset
     * @param len    len
     * @param stdout Indicates if this is stdout
     */
    void writeTestOutput( byte[] buf, int off, int len, boolean stdout );

}