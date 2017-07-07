package builders.loom.plugin.junit4;

public final class TestResult {

    private final boolean successful;

    public TestResult(final boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

}
