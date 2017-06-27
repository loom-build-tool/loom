package jobt.util;

public final class Preconditions {

    private Preconditions() {
    }

    public static void checkState(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void checkState(
        final boolean expression,
        final String errorMessageTemplate,
        final Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalStateException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

}
