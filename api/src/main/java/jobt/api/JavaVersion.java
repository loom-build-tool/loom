package jobt.api;

public enum JavaVersion {

    JAVA_1_5(5, "1.5"),
    JAVA_1_6(6, "1.6"),
    JAVA_1_7(7, "1.7"),
    JAVA_1_8(8, "1.8"),
    JAVA_9(9, "9");

    private final int numericVersion;
    private final String stringVersion;

    JavaVersion(final int numericVersion, final String stringVersion) {
        this.numericVersion = numericVersion;
        this.stringVersion = stringVersion;
    }

    public int getNumericVersion() {
        return numericVersion;
    }

    public String getStringVersion() {
        return stringVersion;
    }

    @SuppressWarnings("checkstyle:returncount")
    public static JavaVersion ofVersion(final String versionStr) {
        switch (versionStr) {
            case "9":
                return JAVA_9;
            case "1.8":
            case "8":
                return JAVA_1_8;
            case "1.7":
            case "7":
                return JAVA_1_7;
            case "1.6":
            case "6":
                return JAVA_1_6;
            case "1.5":
            case "5":
                return JAVA_1_5;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + versionStr);
        }
    }

    public static JavaVersion current() {
        final String javaSpecVersion = System.getProperty("java.specification.version");

        if (javaSpecVersion == null) {
            throw new IllegalStateException("Unknown java.specification.version");
        }

        return ofVersion(javaSpecVersion);
    }

}
