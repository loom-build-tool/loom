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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
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
import builders.loom.util.ClassLoaderUtil;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class JUnit4TestModuleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JUnit4TestModuleTask.class);

    @Override
    public TaskResult run() throws Exception {

        if (!useProduct("testCompilation", CompilationProduct.class).isPresent()) {
            return completeSkip();
        }

        final List<URL> junitClassPath = buildJunitClassPath();

        final URLClassLoader junitUrlClassLoader =
            ClassLoaderUtil.privileged(() ->
            new URLClassLoader(junitClassPath.toArray(new URL[] {}), null));

        final ClassLoader targetClassLoader = ClassLoaderUtil.privileged(
            () -> new SharedApiClassLoader(junitUrlClassLoader,
            new RestrictedClassLoader(JUnit4TestModuleTask.class.getClassLoader())));

        final List<Class<?>> testClasses = collectClasses(targetClassLoader);
        if (testClasses.isEmpty()) {
            return completeSkip();
        }

        final ClassLoader wrappedClassLoader = ClassLoaderUtil.privileged(
            () -> new InjectingClassLoader(
                targetClassLoader, JUnit4TestModuleTask.class.getClassLoader(),
                className -> className.startsWith("builders.loom.plugin.junit4.wrapper.")));

        final Class<?> wrapperClass =
            wrappedClassLoader.loadClass("builders.loom.plugin.junit4.wrapper.JUnit4Wrapper");

        final Object wrapper = wrapperClass.getConstructor().newInstance();
        final Method wrapperRun = wrapperClass.getMethod("run", ClassLoader.class, List.class);

        final TestResult result = (TestResult) wrapperRun.invoke(
            wrapper, targetClassLoader, testClasses);

        if (!result.isSuccessful()) {
            throw new IllegalStateException("JUnit4 report: " + result);
        }

        // note: junit reports are not yet supported, but product expects the folder
        final Path reportPath = Files.createDirectories(
            LoomPaths.reportDir(getBuildContext().getModuleName(), "test"));

        return completeOk(new ReportProduct(reportPath, "Junit4 report"));
    }

    private List<URL> buildJunitClassPath() throws InterruptedException {
        final List<URL> urls = new ArrayList<>();

        useProduct("testCompilation", CompilationProduct.class)
            .map(CompilationProduct::getClassesDir)
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("processedTestResources", ProcessedResourceProduct.class)
            .map(ProcessedResourceProduct::getSrcDir)
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("compilation", CompilationProduct.class)
            .map(CompilationProduct::getClassesDir)
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("processedResources", ProcessedResourceProduct.class)
            .map(ProcessedResourceProduct::getSrcDir)
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("testDependencies", ClasspathProduct.class)
            .map(ClasspathProduct::getEntriesAsUrls)
            .ifPresent(urls::addAll);

        return urls;
    }

    private List<Class<?>> collectClasses(final ClassLoader urlClassLoader)
        throws IOException, InterruptedException {

        final Optional<CompilationProduct> testCompilation = useProduct(
            "testCompilation", CompilationProduct.class);
        if (!testCompilation.isPresent()) {
            return Collections.emptyList();
        }

        final Path rootPath = testCompilation.get().getClassesDir();

        final Class<Annotation> testAnnotation = annotationClass(urlClassLoader);

        return
            Files.walk(rootPath)
                .filter(Files::isRegularFile)
                .map(file -> rootPath.relativize(file).toString())
                .map(ClassLoaderUtil::classnameFromFilename)
                .map(className -> buildClasses(
                    className, testAnnotation, urlClassLoader))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<Class<?>> buildClasses(final String classname,
                              final Class<Annotation> testAnnotation,
                              final ClassLoader classLoader) {

        try {
            final Class<?> clazz = classLoader.loadClass(classname);

            final boolean classHasTestMethod = Arrays.stream(clazz.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(testAnnotation));

            if (!classHasTestMethod) {
                return Optional.empty();
            }
            return Optional.of(clazz);

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

}
