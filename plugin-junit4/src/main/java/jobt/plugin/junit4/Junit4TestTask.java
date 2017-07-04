package jobt.plugin.junit4;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
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

        final URLClassLoader urlClassLoader = buildClassLoader();
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        final Class[] testClasses = collectClasses(urlClassLoader)
            .toArray(new Class[] {});

        final Class<?> wrapperClass = urlClassLoader.loadClass("jobt.plugin.junit4.Junit4Wrapper");
        final Object wrapper = wrapperClass.newInstance();
        final Method wrapperRun = wrapperClass.getMethod("run", Class[].class);

        boolean successful = (boolean) wrapperRun.invoke(wrapper, (Object) testClasses);

        if (successful) {
            return complete(TaskStatus.OK);
        }

        throw new IllegalStateException("Failed");
    }

    private URLClassLoader buildClassLoader() throws MalformedURLException, InterruptedException {
        final List<URL> urls = new ArrayList<>();

        final CompilationProduct testCompilation = getUsedProducts().readProduct(
            "testCompilation", CompilationProduct.class);
        urls.add(testCompilation.getClassesDir().toUri().toURL());

        final ProcessedResourceProduct processedTestResources = getUsedProducts().readProduct(
            "processedTestResources", ProcessedResourceProduct.class);
        urls.add(processedTestResources.getSrcDir().toUri().toURL());

        final CompilationProduct compilation = getUsedProducts().readProduct(
            "compilation", CompilationProduct.class);
        urls.add(compilation.getClassesDir().toUri().toURL());

        final ProcessedResourceProduct processedResources = getUsedProducts().readProduct(
            "processedResources", ProcessedResourceProduct.class);
        urls.add(processedResources.getSrcDir().toUri().toURL());

        final List<URL> testDependencies = getUsedProducts().readProduct("testDependencies",
            ClasspathProduct.class).getEntriesAsUrls();
        urls.addAll(testDependencies);

        System.out.println("Classloader URLs:" + urls);

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
