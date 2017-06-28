package jobt.api;

import java.nio.file.Path;

public class ReportProduct implements Product {

    private final Path reportBaseDir;

    public ReportProduct(final Path reportBaseDir) {
        this.reportBaseDir = reportBaseDir;
    }

    public Path getReportBaseDir() {
        return reportBaseDir;
    }

}
