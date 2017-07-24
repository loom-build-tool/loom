package builders.loom.api;

import java.nio.file.Path;

public interface BuildContext {
    String getModuleName();

    Path getPath();

    BuildConfig getConfig();
}
