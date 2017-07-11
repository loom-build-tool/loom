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

package builders.loom.plugin.checkstyle;

import java.io.File;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

import builders.loom.api.AbstractTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class CheckstyleTask extends AbstractTask {

    private final CompileTarget compileTarget;

    private final CheckstylePluginSettings pluginSettings;
    private final Path cacheDir;
    private final Path reportPath;

    public CheckstyleTask(final CompileTarget compileTarget,
                          final CheckstylePluginSettings pluginSettings,
                          final Path cacheDir) {
        this.compileTarget = compileTarget;
        this.pluginSettings = pluginSettings;
        this.cacheDir = cacheDir;

        reportPath = LoomPaths.REPORT_PATH.resolve(Paths.get("checkstyle",
            compileTarget.name().toLowerCase()));
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<SourceTreeProduct> sourceTree = getSourceTree();

        if (!sourceTree.isPresent()) {
            return completeSkip();
        }

        final List<File> files = sourceTree.get().getSourceFiles().stream()
            .map(Path::toFile)
            .collect(Collectors.toList());

        final RootModule checker = createRootModule();

        try {
            final LoggingAuditListener listener = new LoggingAuditListener();
            checker.addListener(listener);

            Files.createDirectories(reportPath);
            final XMLLogger xmlLogger = new XMLLogger(new PrintStream(reportPath
                .resolve("checkstyle-report.xml").toFile(), "UTF-8"), true);
            checker.addListener(xmlLogger);

            final int errors = checker.process(files);

            if (errors == 0) {
                return completeOk(product());
            }
        } finally {
            checker.destroy();
        }

        throw new IllegalStateException("Checkstyle reported errors!");
    }

    private ReportProduct product() {
        switch (compileTarget) {
            case MAIN:
                return new ReportProduct(reportPath, "Checkstyle main report");
            case TEST:
                return new ReportProduct(reportPath, "Checkstyle test report");
            default:
                throw new IllegalStateException();
        }
    }

    private Optional<SourceTreeProduct> getSourceTree() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("source", SourceTreeProduct.class);
            case TEST:
                return useProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private RootModule createRootModule() throws InterruptedException {
        try {
            final Properties props = createOverridingProperties();
            final Configuration config =
                ConfigurationLoader.loadConfiguration(pluginSettings.getConfigLocation(),
                    new PropertiesExpander(props));

            final ClassLoader moduleClassLoader = Checker.class.getClassLoader();

            final ModuleFactory factory = new PackageObjectFactory(
                Checker.class.getPackage().getName(), moduleClassLoader);

            final RootModule rootModule = (RootModule) factory.createModule(config.getName());
            rootModule.setModuleClassLoader(moduleClassLoader);

            if (rootModule instanceof Checker) {
                final Optional<ClasspathProduct> classpathProduct = buildClassLoader();
                classpathProduct
                    .map(ClasspathProduct::getEntriesAsUrlArray)
                    .map(URLClassLoader::new)
                    .ifPresent(((Checker) rootModule)::setClassLoader);
            }

            rootModule.configure(config);

            return rootModule;
        } catch (final CheckstyleException e) {
            throw new IllegalStateException("Unable to create Root Module with configuration: "
                + pluginSettings.getConfigLocation(), e);
        }
    }

    private Properties createOverridingProperties() {
        final Properties properties = new Properties();

        final Path baseDir = Paths.get("").toAbsolutePath();
        final Path checkstyleConfigDir =
            baseDir.resolve(Paths.get("config", "checkstyle"));

        properties.setProperty("cacheFile", cacheDir.resolve(compileTarget.name().toLowerCase()
             + ".cache").toString());

        // Set the same variables as the checkstyle plugin for eclipse
        // http://eclipse-cs.sourceforge.net/#!/properties
        properties.setProperty("basedir", baseDir.toString());
        properties.setProperty("project_loc", baseDir.toString());
        properties.setProperty("samedir", checkstyleConfigDir.toString());
        properties.setProperty("config_loc", checkstyleConfigDir.toString());

        return properties;
    }

    private Optional<ClasspathProduct> buildClassLoader() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("compileDependencies", ClasspathProduct.class);
            case TEST:
                return useProduct("testDependencies", ClasspathProduct.class);
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

}
