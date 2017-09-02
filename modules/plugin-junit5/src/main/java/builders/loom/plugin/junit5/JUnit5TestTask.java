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

package builders.loom.plugin.junit5;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.plugin.junit5.shared.TestResult;
import builders.loom.plugin.junit5.util.InjectingClassLoader;
import builders.loom.plugin.junit5.util.RestrictedClassLoader;
import builders.loom.plugin.junit5.util.SharedApiClassLoader;
import builders.loom.util.ClassLoaderUtil;

public class JUnit5TestTask extends AbstractModuleTask {

    @Override
    public TaskResult run() throws Exception {
        final Optional<CompilationProduct> testCompilation =
            useProduct("testCompilation", CompilationProduct.class);

        if (!testCompilation.isPresent()) {
            return completeEmpty();
        }

        final Path classesDir = testCompilation.get().getClassesDir();
        final List<URL> junitClassPath = buildJunitClassPath();

        final TestResult result = runTests(classesDir, junitClassPath);

        if (!result.isSuccessful()) {
            throw new IllegalStateException("JUnit report: " + result);
        }

        // note: junit reports are not yet supported, but product expects the folder
        final Path reportDir = resolveReportDir("test");

        return completeOk(new ReportProduct(reportDir, "JUnit report"));
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

        resolveJUnitPlatformLauncher().stream()
            .map(ClassLoaderUtil::toUrl)
            .forEach(urls::add);

        return urls;
    }

    private List<Path> resolveJUnitPlatformLauncher() {
        final DependencyResolverService mavenDependencyResolver = getServiceLocator()
            .getService("mavenDependencyResolver", DependencyResolverService.class);

        final List<String> artifacts = List.of(
            "org.junit.platform:junit-platform-launcher:1.0.0-RC3");

        return mavenDependencyResolver.resolve(artifacts, DependencyScope.COMPILE, "junit");
    }

    private TestResult runTests(final Path classesDir, final List<URL> junitClassPath)
        throws Exception {

        final URLClassLoader junitUrlClassLoader =
            ClassLoaderUtil.privileged(() ->
            new URLClassLoader(junitClassPath.toArray(new URL[] {}), null));

        final ClassLoader targetClassLoader = ClassLoaderUtil.privileged(
            () -> new SharedApiClassLoader(junitUrlClassLoader,
            new RestrictedClassLoader(JUnit5TestTask.class.getClassLoader())));

        final ClassLoader wrappedClassLoader = ClassLoaderUtil.privileged(
            () -> new InjectingClassLoader(
                targetClassLoader, JUnit5TestTask.class.getClassLoader(),
                className -> className.startsWith("builders.loom.plugin.junit5.wrapper.")));

        final Class<?> wrapperClass =
            wrappedClassLoader.loadClass("builders.loom.plugin.junit5.wrapper.JUnit5Wrapper");

        final Object wrapper = wrapperClass.getConstructor().newInstance();
        final Method wrapperRun = wrapperClass.getMethod("run", ClassLoader.class, Path.class);

        return (TestResult) wrapperRun.invoke(wrapper, targetClassLoader, classesDir);
    }

}
