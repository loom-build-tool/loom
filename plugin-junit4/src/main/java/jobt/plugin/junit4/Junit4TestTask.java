package jobt.plugin.junit4;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.AbstractTask;
import jobt.api.TaskStatus;
import jobt.api.product.ClasspathProduct;
import jobt.api.product.CompilationProduct;
import jobt.api.product.ProcessedResourceProduct;
import jobt.api.product.ReportProduct;
import jobt.util.Util;

public class Junit4TestTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(Junit4TestTask.class);

    private static final Path REPORT_PATH = Paths.get("jobtbuild", "reports", "tests");

    @Override
    public TaskStatus run() throws Exception {
        final CompilationProduct testCompilation = getUsedProducts().readProduct(
            "testCompilation", CompilationProduct.class);

        if (Files.notExists(testCompilation.getClassesDir())) {
            return complete(TaskStatus.SKIP);
        }

        final URLClassLoader targetClassLoader = buildClassLoader();

        final Class[] testClasses = collectClasses(targetClassLoader)
            .toArray(new Class[] {});

        final ClassLoader wrappedClassLoader = new InjectingClassLoader(
            targetClassLoader, Junit4TestTask.class.getClassLoader(),
            className -> className.startsWith("jobt.plugin.junit4.wrapper."));

        final Class<?> wrapperClass =
            wrappedClassLoader.loadClass("jobt.plugin.junit4.wrapper.Junit4Wrapper");
        final Object wrapper = wrapperClass.newInstance();
        final Method wrapperRun = wrapperClass.getMethod("run", Class[].class);

        final TestResult result = (TestResult) wrapperRun.invoke(wrapper, (Object) testClasses);

        if (result.isSuccessful()) {
            return complete(TaskStatus.OK);
        }

        throw new IllegalStateException("Some tests failed");
    }

    private URLClassLoader buildClassLoader() throws InterruptedException {
        final List<Path> paths = new ArrayList<>();

        final CompilationProduct testCompilation = getUsedProducts().readProduct(
            "testCompilation", CompilationProduct.class);
        paths.add(testCompilation.getClassesDir());

        final ProcessedResourceProduct processedTestResources = getUsedProducts().readProduct(
            "processedTestResources", ProcessedResourceProduct.class);
        paths.add(processedTestResources.getSrcDir());

        final CompilationProduct compilation = getUsedProducts().readProduct(
            "compilation", CompilationProduct.class);
        paths.add(compilation.getClassesDir());

        final ProcessedResourceProduct processedResources = getUsedProducts().readProduct(
            "processedResources", ProcessedResourceProduct.class);
        paths.add(processedResources.getSrcDir());

        final List<URL> testDependencies = getUsedProducts().readProduct("testDependencies",
            ClasspathProduct.class).getEntriesAsUrls();

        final List<URL> urls = new ArrayList<>();

        paths.stream()
            .filter(p -> Files.isDirectory(p))
            .map(Util::toUrl)
            .forEach(urls::add);
        urls.addAll(testDependencies);

        // TODO remove
        System.out.println();
        System.out.println();
        urls.forEach(u -> System.out.println(" - " + u));

        System.out.println();
        System.out.println();

        return new URLClassLoader(urls.toArray(new URL[] {}),
            Junit4TestTask.class.getClassLoader());
    }

    private List<Class<?>> collectClasses(final URLClassLoader urlClassLoader)
        throws IOException, InterruptedException {

        final List<Class<?>> classes = new ArrayList<>();

        final CompilationProduct testCompilation = getUsedProducts().readProduct(
            "testCompilation", CompilationProduct.class);

        final Path rootPath = testCompilation.getClassesDir();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {

                final String filename = rootPath.relativize(file).toString();
                buildClasses(Util.classnameFromFilename(filename), urlClassLoader, classes);

                return FileVisitResult.CONTINUE;
            }
        });

        return classes;
    }

    private void buildClasses(final String classname,
                              final URLClassLoader urlClassLoader, final List<Class<?>> classes) {

        final Class<? extends Annotation> testAnnotation;
        try {
            testAnnotation = (Class<? extends Annotation>)
                urlClassLoader.loadClass("org.junit.Test");
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        try {
            final Class<?> clazz = urlClassLoader.loadClass(classname);

            final boolean classHasTestMethod = Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(testAnnotation));

            if (classHasTestMethod) {
                classes.add(clazz);
            }

        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private TaskStatus complete(final TaskStatus status) {
        getProvidedProducts().complete("test", new ReportProduct(REPORT_PATH));
        return status;
    }

}
