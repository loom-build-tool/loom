package builders.loom.plugin.mavenresolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressIndicator.class);

    private final String taskDescription;

    public ProgressIndicator(final String taskDescription) {
        this.taskDescription = taskDescription;
    }

    /**
     * Callers should provide a human-readable string stating the progress made so far.
     *
     * Example: "downloaded 5 (of 10 total) artifacts from maven repo central"
     * Full report:
     *  Maven Resolver (running 2,4s): downloaded 5 ....
     */
    public void reportProgress(final String progressMessage) {
        LOG.debug("Got progress message for <{}>: {}",
            taskDescription, progressMessage);
    }

}
