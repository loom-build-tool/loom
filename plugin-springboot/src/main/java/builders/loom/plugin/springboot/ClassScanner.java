package builders.loom.plugin.springboot;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicReference;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;

public class ClassScanner {

    public String scanArchives(final Path dir, final String annotation) throws IOException {
        final AtomicReference<String> atomicReference = new AtomicReference<>();

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {

                final String className = readFile(file, annotation);
                if (className != null) {
                    atomicReference.set(className);
                    return FileVisitResult.TERMINATE;
                }

                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(dir, visitor);

        return atomicReference.get();
    }

    private String readFile(final Path file, final String annotation) throws IOException {
        try (final DataInputStream dataInputStream = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(file)))) {

            final ClassFile classFile = new ClassFile(dataInputStream);
            final String className = classFile.getName();
            final AnnotationsAttribute visible =
                (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.visibleTag);

            if (visible == null) {
                return null;
            }

            return visible.getAnnotation(annotation) != null ? className : null;
        }
    }

}
