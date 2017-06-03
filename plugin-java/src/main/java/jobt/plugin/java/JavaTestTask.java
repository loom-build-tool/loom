package jobt.plugin.java;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.Task;
import jobt.api.TaskStatus;

public class JavaTestTask implements Task {

    private static final Logger LOG = LoggerFactory.getLogger(JavaTestTask.class);

    private final List<File> classPath;

    public JavaTestTask(final List<File> classPath) {
        this.classPath = classPath;
    }

    @Override
    public TaskStatus run() throws Exception {
        if (Files.notExists(Paths.get("jobtbuild/classes/test"))) {
            return TaskStatus.SKIP;
        }

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();

        final URLClassLoader urlClassLoader = buildClassLoader();

        final List<Class<?>> fooTest = collectClasses(urlClassLoader);

        final Result run = jUnitCore.run(computer, fooTest.toArray(new Class<?>[] {}));

        if (run.wasSuccessful()) {
            return TaskStatus.OK;
        }

        for (final Failure failure : run.getFailures()) {
            LOG.error(failure.toString());
        }

        throw new IllegalStateException("Failed");
    }

    private URLClassLoader buildClassLoader() throws MalformedURLException {
        final List<URL> urls = new ArrayList<>();
        urls.add(Paths.get("jobtbuild/classes/test").toUri().toURL());
        urls.add(Paths.get("jobtbuild/classes/main").toUri().toURL());
        urls.add(Paths.get("jobtbuild/resources/test").toUri().toURL());
        urls.add(Paths.get("jobtbuild/resources/main").toUri().toURL());

        for (final File file : classPath) {
            urls.add(file.toURI().toURL());
        }

        return new URLClassLoader(urls.toArray(new URL[] {}));
    }

    private List<Class<?>> collectClasses(final URLClassLoader urlClassLoader)
        throws ClassNotFoundException, IOException {

        final List<Class<?>> classes = new ArrayList<>();

        final Path rootPath = Paths.get("jobtbuild/classes/test");
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {

                buildClasses(file, rootPath, urlClassLoader, classes);
                return FileVisitResult.CONTINUE;
            }
        });

        return classes;
    }

    private void buildClasses(final Path file, final Path rootPath,
                              final URLClassLoader urlClassLoader, final List<Class<?>> classes) {
        final String filename = rootPath.relativize(file).toString();
        final String classname = filename
            .substring(0, filename.indexOf(".class"))
            .replace(File.separatorChar, '.');

        try {
            final Class<?> clazz = Class.forName(classname, true, urlClassLoader);

            boolean testClass = false;
            for (final Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    testClass = true;
                    break;
                }
            }

            if (testClass) {
                classes.add(clazz);
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
