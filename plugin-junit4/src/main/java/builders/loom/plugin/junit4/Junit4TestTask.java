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

import java.io.File;
import java.io.IOException;
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
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractTask;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ReportProduct;

public class Junit4TestTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(Junit4TestTask.class);

    private static final Path REPORT_PATH = Paths.get("loombuild", "reports", "tests");

    @Override
    public TaskResult run() throws Exception {
        final Optional<CompilationProduct> testCompilationProduct = useProduct(
            "testCompilation", CompilationProduct.class);

        if (!testCompilationProduct.isPresent()) {
            return completeSkip();
        }

        final CompilationProduct testCompilation = testCompilationProduct.get();
        final URLClassLoader urlClassLoader = buildClassLoader(testCompilation);
        final Class<?>[] testClasses = collectClasses(urlClassLoader, testCompilation)
            .toArray(new Class<?>[] {});

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new RunListener() {
            @Override
            public void testFailure(final Failure failure) throws Exception {
                LOG.error("Test failure: {}", failure);
            }
        });
        final Result run = jUnitCore.run(computer, testClasses);

        if (!run.wasSuccessful()) {
            throw new IllegalStateException("Failed");
        }

        return completeOk(new ReportProduct(REPORT_PATH, "Junit test report"));
    }

    private URLClassLoader buildClassLoader(final CompilationProduct testCompilation)
        throws MalformedURLException, InterruptedException {

        final List<URL> urls = new ArrayList<>();
        urls.add(testCompilation.getClassesDir().toUri().toURL());

        final Optional<ProcessedResourceProduct> processedTestResources = useProduct(
            "processedTestResources", ProcessedResourceProduct.class);
        if (processedTestResources.isPresent()) {
            urls.add(processedTestResources.get().getSrcDir().toUri().toURL());
        }

        final Optional<CompilationProduct> compilation = useProduct(
            "compilation", CompilationProduct.class);
        if (compilation.isPresent()) {
            urls.add(compilation.get().getClassesDir().toUri().toURL());
        }

        final Optional<ProcessedResourceProduct> processedResources = useProduct(
            "processedResources", ProcessedResourceProduct.class);
        if (processedResources.isPresent()) {
            urls.add(processedResources.get().getSrcDir().toUri().toURL());
        }

        final Optional<ClasspathProduct> testDependencies = useProduct(
            "testDependencies", ClasspathProduct.class);
        if (testDependencies.isPresent()) {
            urls.addAll(testDependencies.get().getEntriesAsUrls());
        }

        return new URLClassLoader(urls.toArray(new URL[] {}),
            Junit4TestTask.class.getClassLoader());
    }

    private List<Class<?>> collectClasses(final URLClassLoader urlClassLoader,
                                          final CompilationProduct testCompilation)
        throws IOException {

        final List<Class<?>> classes = new ArrayList<>();

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

        try {
            final Class<?> clazz = Class.forName(classname, true, urlClassLoader);

            final boolean classHasTestMethod = Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(Test.class));

            if (classHasTestMethod) {
                classes.add(clazz);
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
