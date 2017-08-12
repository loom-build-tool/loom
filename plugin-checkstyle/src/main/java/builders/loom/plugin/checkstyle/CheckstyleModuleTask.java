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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class CheckstyleModuleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(CheckstyleModuleTask.class);

    private final CompileTarget compileTarget;

    private final CheckstylePluginSettings pluginSettings;
    private final Path cacheDir;

    public CheckstyleModuleTask(final CompileTarget compileTarget,
                                final CheckstylePluginSettings pluginSettings,
                                final Path cacheDir) {
        this.compileTarget = compileTarget;
        this.pluginSettings = pluginSettings;
        this.cacheDir = cacheDir;

        if (pluginSettings.getConfigLocation() == null) {
            throw new IllegalStateException("Missing configuration: checkstyle.configLocation");
        }
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<SourceTreeProduct> sourceTree = getSourceTree();

        if (!sourceTree.isPresent()) {
            return completeSkip();
        }

        // Checkstyle doesn't support module-info.java, so skip it
        final List<File> files = sourceTree.get().getSourceFiles().stream()
            .map(Path::toFile)
            .filter(f -> !f.getName().equals("module-info.java"))
            .collect(Collectors.toList());

        final Path reportPath = LoomPaths.reportDir(getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(), "checkstyle")
            .resolve(compileTarget.name().toLowerCase());

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
                return completeOk(product(reportPath));
            }
        } finally {
            checker.destroy();
        }

        throw new IllegalStateException("Checkstyle reported errors!");
    }

    private ReportProduct product(final Path reportPath) {
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
        final String configLocation = determineConfigLocation();
        LOG.debug("Read config from {}", configLocation);

        try {
            final Properties props = createOverridingProperties(configLocation);

            LOG.debug("Checkstyle properties: {}", props);

            final Configuration config =
                ConfigurationLoader.loadConfiguration(configLocation,
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
                + configLocation, e);
        }
    }

    private String determineConfigLocation() {
        final String configLocation = pluginSettings.getConfigLocation();
        if (configLocation.startsWith("/")) {
            // embedded configuration
            return configLocation;
        }

        return getBuildContext().getPath().resolve(configLocation)
            .toAbsolutePath().normalize().toString();
    }

    private Properties createOverridingProperties(final String configLocation) {
        final Properties properties = new Properties();

        properties.setProperty("cacheFile", cacheDir
            .resolve(getBuildContext().getModuleName())
            .resolve(compileTarget.name().toLowerCase()+ ".cache")
            .toAbsolutePath().normalize().toString());

        // Set the same variables as the checkstyle plugin for eclipse
        // http://eclipse-cs.sourceforge.net/#!/properties
        final Path baseDir = getRuntimeConfiguration().getProjectBaseDir()
            .toAbsolutePath().normalize();
        properties.setProperty("basedir", baseDir.toString());
        properties.setProperty("project_loc", baseDir.toString());

        if (!pluginSettings.getConfigLocation().startsWith("/")) {
            final Path checkstyleConfigDir = Paths.get(configLocation).getParent();
            properties.setProperty("samedir", checkstyleConfigDir.toString());
            properties.setProperty("config_loc", checkstyleConfigDir.toString());
        }

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
