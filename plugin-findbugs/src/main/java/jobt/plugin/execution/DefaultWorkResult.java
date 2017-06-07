package jobt.plugin.execution;

import java.io.Serializable;

public class DefaultWorkResult implements WorkResult, Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean didWork;
    private final Throwable exception;

    public DefaultWorkResult(final boolean didWork, final Throwable exception) {
        this.didWork = didWork;
        this.exception = exception;
    }

    @Override
    public boolean getDidWork() {
        return didWork;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isSuccess() {
        return exception == null;
    }
}