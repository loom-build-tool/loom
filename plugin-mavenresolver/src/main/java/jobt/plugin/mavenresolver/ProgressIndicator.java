package jobt.plugin.mavenresolver;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO extract interface
public class ProgressIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressIndicator.class);

    private final String taskDescription;

    private final AtomicReference<String> currentProgressMessage = new AtomicReference<>();

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
    synchronized public void reportProgress(final String progressMessage) {
        LOG.debug("Got progress message for <{}>: {}",
            taskDescription, progressMessage);
        currentProgressMessage.set(progressMessage);
    }

    public String getCurrentProgressMessage() {
        return currentProgressMessage.get();
    }

}
