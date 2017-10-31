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

package builders.loom.plugin.junit;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskResult;
import builders.loom.api.TestProgressEmitter;
import builders.loom.api.TestProgressEmitterAware;
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.plugin.junit.shared.ProgressListenerDelegate;
import builders.loom.plugin.junit.shared.TestResult;
import builders.loom.plugin.junit.util.InjectingClassLoader;
import builders.loom.plugin.junit.util.SharedApiClassLoader;
import builders.loom.util.ClassLoaderUtil;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class JUnitTestTask extends AbstractModuleTask implements TestProgressEmitterAware {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitTestTask.class);

    private final DependencyResolverService dependencyResolverService;
    private TestProgressEmitter testProgressEmitter;

    public JUnitTestTask(final DependencyResolverService dependencyResolverService) {
        this.dependencyResolverService = dependencyResolverService;
    }

    @Override
    public void setTestProgressEmitter(final TestProgressEmitter testProgressEmitter) {
        this.testProgressEmitter = testProgressEmitter;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<Product> testCompilation =
            useProduct("testCompilation", Product.class);

        if (!testCompilation.isPresent()) {
            return TaskResult.empty();
        }

        final Path classesDir = Paths.get(testCompilation.get().getProperty("classesDir"));
        final List<URL> junitClassPath = buildJunitClassPath();

        LOG.debug("Test with classpath: {}", junitClassPath);

        final Path reportDir = FileUtil.createOrCleanDirectory(resolveReportDir("test"));

        final TestResult result = runTests(classesDir, junitClassPath, reportDir);

        LOG.info("JUnit test result: {}", result);

        if (result.getTotalFailureCount() > 0) {
            return TaskResult.fail(newProduct(reportDir),
                String.format(
                "tests failed: %d (total: %d; succeeded: %d; skipped: %d; aborted: %d)",
                result.getTotalFailureCount(),
                result.getTestsFoundCount(),
                result.getTestsSucceededCount(),
                result.getTestsSkippedCount(),
                result.getTestsAbortedCount()));
        }

        return TaskResult.done(newProduct(reportDir));
    }

    private List<URL> buildJunitClassPath() throws InterruptedException {
        final List<URL> urls = new ArrayList<>();

        useProduct("testCompilation", Product.class)
            .map(p -> Paths.get(p.getProperty("classesDir")))
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("processedTestResources", Product.class)
            .map(p -> Paths.get(p.getProperty("processedResourcesDir")))
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("compilation", Product.class)
            .map(p -> Paths.get(p.getProperty("classesDir")))
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("processedResources", Product.class)
            .map(p -> Paths.get(p.getProperty("processedResourcesDir")))
            .map(ClassLoaderUtil::toUrl)
            .ifPresent(urls::add);

        useProduct("testDependencies", Product.class)
            .map(p -> p.getProperties("classpath"))
            .ifPresent(p -> p.forEach(c -> urls.add(ClassLoaderUtil.toUrl(Paths.get(c)))));

        for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
            useProduct(moduleName, "compilation", Product.class)
                .map(p -> Paths.get(p.getProperty("classesDir")))
                .ifPresent(classesDir -> urls.add(ClassLoaderUtil.toUrl(classesDir)));
        }

        resolveJUnitPlatformLauncher().stream()
            .map(ClassLoaderUtil::toUrl)
            .forEach(urls::add);

        return urls;
    }

    private List<Path> resolveJUnitPlatformLauncher() {
        final List<String> artifacts = List.of(
            "org.junit.platform:junit-platform-launcher:1.0.0");

        return dependencyResolverService.resolveMainArtifacts(artifacts, DependencyScope.COMPILE);
    }

    private TestResult runTests(final Path classesDir, final List<URL> junitClassPath,
                                final Path reportDir) throws Exception {

        // SpotBugs warns if not using ClassLoaderUtil.privileged

        try (URLClassLoader junitUrlClassLoader = ClassLoaderUtil.privileged(
            () -> new URLClassLoader(junitClassPath.toArray(new URL[] {}),
                ClassLoader.getPlatformClassLoader()))) {

            final ClassLoader targetClassLoader = ClassLoaderUtil.privileged(
                () -> new SharedApiClassLoader(junitUrlClassLoader,
                    JUnitTestTask.class.getClassLoader()));

            final ClassLoader wrappedClassLoader = ClassLoaderUtil.privileged(
                () -> new InjectingClassLoader(targetClassLoader,
                    JUnitTestTask.class.getClassLoader(),
                    className -> className.startsWith("builders.loom.plugin.junit.wrapper.")));

            final Class<?> wrapperClass =
                wrappedClassLoader.loadClass("builders.loom.plugin.junit.wrapper.JUnitWrapper");

            final Object wrapper = wrapperClass.getConstructor().newInstance();
            final Method wrapperRun = wrapperClass.getMethod("run",
                ClassLoader.class, Path.class, Path.class, ProgressListenerDelegate.class);

            return (TestResult) wrapperRun.invoke(wrapper, targetClassLoader, classesDir,
                reportDir, new ProgressListenerDelegate(testProgressEmitter));
        }
    }

    private static Product newProduct(final Path reportDir) {
        return new GenericProduct("reportDir", reportDir.toString(),
            ProductChecksumUtil.recursiveMetaChecksum(reportDir),
            new OutputInfo("JUnit report", reportDir.toString()));
    }

}
