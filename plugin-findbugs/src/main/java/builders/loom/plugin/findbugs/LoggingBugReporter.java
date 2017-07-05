package builders.loom.plugin.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class LoggingBugReporter extends AbstractBugReporter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingBugReporter.class);

    @Override
    protected void doReportBug(final BugInstance bugInstance) {
        LOG.error("Findbugs bug: {}", bugInstance);
    }

    @Override
    public void reportAnalysisError(final AnalysisError error) {
        LOG.error("Findbugs analysis error: {}", error);
    }

    @Override
    public void reportMissingClass(final String string) {
        LOG.error("Missing class: {}", string);
    }

    @Override
    public void finish() {

    }

    @Override
    public BugCollection getBugCollection() {
        return null;
    }

    @Override
    public void observeClass(final ClassDescriptor classDescriptor) {

    }

}
