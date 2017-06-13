package jobt.util;

public class UncheckedExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UncheckedExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public UncheckedExecutionException(final Throwable cause) {
        super(cause);
    }

}
