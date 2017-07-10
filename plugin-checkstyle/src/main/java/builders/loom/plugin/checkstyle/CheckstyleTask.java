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
import builders.loom.api.TaskStatus;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class CheckstyleTask extends AbstractTask {

    public static final Path REPORT_PATH = Paths.get("loombuild", "reports", "checkstyle");

    private final CompileTarget compileTarget;
    private final CheckstylePluginSettings pluginSettings;

    public CheckstyleTask(final CompileTarget compileTarget,
                          final CheckstylePluginSettings pluginSettings) {
        this.compileTarget = compileTarget;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskStatus run() throws Exception {
        final SourceTreeProduct sourceTree = getSourceTree();

        if (Files.notExists(sourceTree.getSrcDir())) {
            return complete(TaskStatus.SKIP);
        }

        final List<File> files = Files.walk(sourceTree.getSrcDir())
            .filter(Files::isRegularFile)
            .map(Path::toFile)
            .collect(Collectors.toList());

        final RootModule checker = createRootModule();

        try {
            final LoggingAuditListener listener = new LoggingAuditListener();
            checker.addListener(listener);

            Files.createDirectories(REPORT_PATH);
            final XMLLogger xmlLogger = new XMLLogger(
                new PrintStream(REPORT_PATH.resolve("checkstyle-report.xml").toFile(), "UTF-8"), true);
            checker.addListener(xmlLogger);

            final int errors = checker.process(files);

            if (errors == 0) {
                return complete(TaskStatus.OK);
            }
        } finally {
            checker.destroy();
        }

        throw new IllegalStateException("Checkstyle reported errors!");
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("checkstyleMainReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("checkstyleTestReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private SourceTreeProduct getSourceTree() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("source", SourceTreeProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testSource", SourceTreeProduct.class);
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
                final ClassLoader loader = buildClassLoader();

                ((Checker) rootModule).setClassLoader(loader);
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

        properties.setProperty("cacheFile", String.format(".loom/checkstyle/%s.cache",
            compileTarget.name().toLowerCase()));

        // Set the same variables as the checkstyle plugin for eclipse
        // http://eclipse-cs.sourceforge.net/#!/properties
        properties.setProperty("basedir", baseDir.toString());
        properties.setProperty("project_loc", baseDir.toString());
        properties.setProperty("samedir", checkstyleConfigDir.toString());
        properties.setProperty("config_loc", checkstyleConfigDir.toString());

        return properties;
    }

    private URLClassLoader buildClassLoader() throws InterruptedException {
        final ClasspathProduct classpath;
        switch (compileTarget) {
            case MAIN:
                classpath = getUsedProducts().readProduct(
                    "compileDependencies", ClasspathProduct.class);
                break;
            case TEST:
                classpath = getUsedProducts().readProduct(
                    "testDependencies", ClasspathProduct.class);
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

        return new URLClassLoader(classpath.getEntriesAsUrlArray());
    }

}
