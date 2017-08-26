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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
public class CheckstyleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(CheckstyleTask.class);

    private final CompileTarget compileTarget;

    private final CheckstylePluginSettings pluginSettings;
    private final Path cacheDir;
    private final String sourceProductId;
    private final String classpathProductId;
    private final String reportOutputDescription;

    public CheckstyleTask(final CompileTarget compileTarget,
                          final CheckstylePluginSettings pluginSettings,
                          final Path cacheDir) {
        this.compileTarget = compileTarget;
        this.pluginSettings = pluginSettings;
        this.cacheDir = cacheDir;

        if (pluginSettings.getConfigLocation() == null) {
            throw new IllegalStateException("Missing configuration: checkstyle.configLocation");
        }

        switch (compileTarget) {
            case MAIN:
                sourceProductId = "source";
                classpathProductId = "compileDependencies";
                reportOutputDescription = "Checkstyle main report";
                break;
            case TEST:
                sourceProductId = "testSource";
                classpathProductId = "testDependencies";
                reportOutputDescription = "Checkstyle test report";
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public TaskResult run() throws Exception {

        final List<File> files = listSourceFiles();

        if (files.isEmpty()) {
            return completeEmpty();
        }

        LOG.info("Start analyzing {} source files with Checkstyle", files.size());

        final Path reportDir =
            Files.createDirectories(resolveReportDir("checkstyle", compileTarget));

        final RootModule rootModule = createRootModule();
        rootModule.addListener(new LoggingAuditListener());
        rootModule.addListener(newXmlLogger(reportDir.resolve("checkstyle-report.xml")));

        try {
            final int errors = rootModule.process(files);

            if (errors > 0) {
                throw new IllegalStateException("Checkstyle reported " + errors + " errors");
            }
        } finally {
            rootModule.destroy();
        }

        return completeOk(new ReportProduct(reportDir, reportOutputDescription));
    }

    private List<File> listSourceFiles() throws InterruptedException {
        final Optional<SourceTreeProduct> sourceTree =
            useProduct(sourceProductId, SourceTreeProduct.class);

        if (!sourceTree.isPresent()) {
            return Collections.emptyList();
        }

        // Checkstyle doesn't support module-info.java, so skip it
        return sourceTree.get().getSrcFiles().stream()
            .filter(f -> !f.getFileName().toString().equals(LoomPaths.MODULE_INFO_JAVA))
            .map(Path::toFile)
            .collect(Collectors.toList());
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
                useProduct(classpathProductId, ClasspathProduct.class)
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
        if (isEmbeddedConfiguration(configLocation)) {
            return configLocation;
        }

        return getBuildContext().getPath().resolve(configLocation)
            .toAbsolutePath().normalize().toString();
    }

    private boolean isEmbeddedConfiguration(final String configLocation) {
        return configLocation.startsWith("/");
    }

    private Properties createOverridingProperties(final String configLocation) {
        final Properties properties = new Properties();

        final Path cacheFile = cacheDir
            .resolve(getBuildContext().getModuleName())
            .resolve(compileTarget.name().toLowerCase())
            .resolve("checkstyle.cache");

        properties.setProperty("cacheFile", cacheFile
            .toAbsolutePath().normalize().toString());

        // Set the same variables as the checkstyle plugin for eclipse
        // http://eclipse-cs.sourceforge.net/#!/properties
        final Path baseDir = getRuntimeConfiguration().getProjectBaseDir()
            .toAbsolutePath().normalize();
        properties.setProperty("basedir", baseDir.toString());
        properties.setProperty("project_loc", baseDir.toString());

        if (!isEmbeddedConfiguration(pluginSettings.getConfigLocation())) {
            final Path checkstyleConfigDir = Paths.get(configLocation).getParent();
            properties.setProperty("samedir", checkstyleConfigDir.toString());
            properties.setProperty("config_loc", checkstyleConfigDir.toString());
        }

        return properties;
    }

    private XMLLogger newXmlLogger(final Path reportFile) throws IOException {
        return new XMLLogger(new BufferedOutputStream(Files.newOutputStream(reportFile)), true);
    }

}
