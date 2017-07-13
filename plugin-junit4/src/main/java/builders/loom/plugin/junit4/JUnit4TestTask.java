/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.plugin.junit4;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.plugin.junit4.shared.TestResult;
import builders.loom.plugin.junit4.util.InjectingClassLoader;
import builders.loom.plugin.junit4.util.RestrictedClassLoader;
import builders.loom.plugin.junit4.util.SharedApiClassLoader;
import builders.loom.util.Util;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class JUnit4TestTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(JUnit4TestTask.class);

    private static final Path REPORT_PATH = LoomPaths.REPORT_PATH.resolve("tests").resolve("test");

    @Override
    public TaskResult run() throws Exception {

        if (!useProduct("testCompilation", CompilationProduct.class).isPresent()) {
            return completeSkip();
        }

        final List<URL> junitClassPath = buildJunitClassPath();

        final URLClassLoader junitUrlClassLoader =
            privileged(() -> new URLClassLoader(junitClassPath.toArray(new URL[] {}), null));

        final ClassLoader targetClassLoader =
            AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return new SharedApiClassLoader(junitUrlClassLoader,
                        new RestrictedClassLoader(JUnit4TestTask.class.getClassLoader()));
                }
            });

        final List<Class<?>> testClasses = collectClasses(targetClassLoader);
        if (testClasses.isEmpty()) {
            return completeSkip();
        }

        final ClassLoader wrappedClassLoader = privileged(() -> new InjectingClassLoader(
            targetClassLoader, JUnit4TestTask.class.getClassLoader(),
            className -> className.startsWith("builders.loom.plugin.junit4.wrapper.")));

        final Class<?> wrapperClass =
            wrappedClassLoader.loadClass("builders.loom.plugin.junit4.wrapper.JUnit4Wrapper");

        final Object wrapper = wrapperClass.newInstance();
        final Method wrapperRun = wrapperClass.getMethod("run", ClassLoader.class, List.class);

        final TestResult result = (TestResult) wrapperRun.invoke(
            wrapper, targetClassLoader, testClasses);

        if (!result.isSuccessful()) {
            throw new IllegalStateException("JUnit4 report: " + result);
        }

        Files.createDirectories(REPORT_PATH);

        return completeOk(new ReportProduct(REPORT_PATH, "Junit4 report"));
    }

    private List<URL> buildJunitClassPath() throws InterruptedException {
        final List<URL> urls = new ArrayList<>();

        useProduct("testCompilation", CompilationProduct.class)
            .map(CompilationProduct::getClassesDir)
            .map(Util::toUrl)
            .ifPresent(urls::add);

        useProduct("processedTestResources", ProcessedResourceProduct.class)
            .map(ProcessedResourceProduct::getSrcDir)
            .map(Util::toUrl)
            .ifPresent(urls::add);

        useProduct("compilation", CompilationProduct.class)
            .map(CompilationProduct::getClassesDir)
            .map(Util::toUrl)
            .ifPresent(urls::add);

        useProduct("processedResources", ProcessedResourceProduct.class)
            .map(ProcessedResourceProduct::getSrcDir)
            .map(Util::toUrl)
            .ifPresent(urls::add);

        useProduct("testDependencies", ClasspathProduct.class)
            .map(ClasspathProduct::getEntriesAsUrls)
            .ifPresent(urls::addAll);

        return urls;
    }

    private List<Class<?>> collectClasses(final ClassLoader urlClassLoader)
        throws IOException, InterruptedException {

        final List<Class<?>> classes = new ArrayList<>();

        final Optional<CompilationProduct> testCompilation = useProduct(
            "testCompilation", CompilationProduct.class);
        if (!testCompilation.isPresent()) {
            return Collections.emptyList();
        }

        final Path rootPath = testCompilation.get().getClassesDir();
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
                              final ClassLoader classLoader, final List<Class<?>> classes) {

        final Class<Annotation> testAnnotation = annotationClass(classLoader);

        try {
            final Class<?> clazz = classLoader.loadClass(classname);

            final boolean classHasTestMethod = Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(testAnnotation));

            if (classHasTestMethod) {
                classes.add(clazz);
            }

        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<Annotation> annotationClass(final ClassLoader classLoader) {
        try {
            return (Class<Annotation>)
                classLoader.loadClass("org.junit.Test");
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <CT extends ClassLoader> CT privileged(
        final Supplier<CT> classLoaderSupplier) {
        return
            AccessController.doPrivileged(new PrivilegedAction<CT>() {
                @Override
                public CT run() {
                    return classLoaderSupplier.get();
                }
            });
    }

}
