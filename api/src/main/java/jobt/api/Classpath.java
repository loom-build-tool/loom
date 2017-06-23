package jobt.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Must be immutable!
 */
public final class Classpath implements Product {

    private final List<Path> entries;

    public Classpath(final Path entry) {
        Objects.requireNonNull(entry);
        this.entries = Collections.unmodifiableList(
            Collections.singletonList(entry));
    }

    public Classpath(final List<Path> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<Path> getEntries() {
        return entries;
    }

    public URL[] getEntriesAsUrlArray() {
        return
            entries.stream()
                .map(Classpath::toURL)
                .toArray(URL[]::new);
    }

    public List<URL> getEntriesAsUrls() {
        return
            entries.stream()
                .map(Classpath::toURL)
                .collect(Collectors.toList());
    }

    private static URL toURL(final Path p) {
        try {
            return p.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Path getSingleEntry() {
        if (entries.size() != 1) {
            throw new IllegalStateException();
        }
        return entries.get(0);
    }

    @Override
    public String toString() {
        return entries.toString();
    }


}
