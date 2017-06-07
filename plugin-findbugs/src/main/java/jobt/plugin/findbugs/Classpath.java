package jobt.plugin.findbugs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Classpath {

    private final List<Path> entries;

    public Classpath(final List<Path> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<Path> getEntries() {
        return entries;
    }

    public Path getSingleEntry() {
        if (entries.size() != 1) {
            throw new IllegalStateException();
        }
        return entries.get(0);
    }

}
