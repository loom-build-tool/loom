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

package builders.loom.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.core.config.BuildConfigImpl;
import builders.loom.core.config.ConfigReader;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.core.plugin.PluginLoader;
import builders.loom.core.service.ServiceLoader;
import builders.loom.core.service.ServiceRegistryImpl;
import builders.loom.util.Stopwatch;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class LoomProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LoomProcessor.class);

    private ModuleRunner moduleRunner;

    static {
        System.setProperty("loom.version", LoomVersion.getVersion());
    }

    public void init(final RuntimeConfigurationImpl runtimeConfiguration,
                     final ProgressMonitor progressMonitor) {
        final Stopwatch sw = new Stopwatch();

        // Init Modules / Plugins for Modules
        final ModuleRegistry moduleRegistry = new ModuleRegistry();
        listModules(runtimeConfiguration).forEach(moduleRegistry::register);

        LOG.debug("Initialized modules in {}", sw);
        sw.reset();

        final ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl();
        new ServiceLoader(runtimeConfiguration, progressMonitor, serviceRegistry).initServices();

        LOG.debug("Initialized services in {}", sw);
        sw.reset();

        final PluginLoader pluginLoader = new PluginLoader(runtimeConfiguration,
                serviceRegistry);
        moduleRunner = new ModuleRunner(
            runtimeConfiguration, serviceRegistry, pluginLoader, moduleRegistry,
            progressMonitor, new TestProgressEmitterBridge(progressMonitor));
        moduleRunner.init();

        LOG.debug("Initialized ModuleRunner in {}", sw);
    }

    public ModuleRunner getModuleRunner() {
        return moduleRunner;
    }

    private List<Module> listModules(final RuntimeConfigurationImpl runtimeConfiguration) {
        final List<Module> modules = new ArrayList<>();

        if (runtimeConfiguration.isModuleBuild()) {
            modules.addAll(scanForModules(runtimeConfiguration));
        } else {
            modules.add(singleModule(runtimeConfiguration));
        }

        return modules;
    }

    private Module singleModule(final RuntimeConfigurationImpl rtConfig) {
        final Path configFile = rtConfig.getProjectBaseDir().resolve("module.yml");

        final ModuleBuildConfig buildConfig;
        if (Files.exists(configFile)) {
            buildConfig = ConfigReader.readConfig(rtConfig, configFile, "base");
        } else {
            buildConfig = new BuildConfigImpl();
        }

        final String moduleName = findModuleName(rtConfig.getProjectBaseDir(), buildConfig, null)
            .orElse(Module.UNNAMED_MODULE);

        return new Module(moduleName, rtConfig.getProjectBaseDir(), buildConfig);
    }

    @SuppressWarnings("checkstyle:returncount")
    private Optional<String> findModuleName(final Path projectDir,
                                            final ModuleBuildConfig buildConfig,
                                            final String pathName) {

        final Optional<String> moduleNameFromModuleInfo = readModuleNameFromModuleInfo(projectDir);

        final Optional<String> moduleNameFromYml =
            Optional.ofNullable(buildConfig.getBuildSettings().getModuleName());

        if (moduleNameFromModuleInfo.isPresent()) {
            if (moduleNameFromYml.isPresent()) {
                throw new IllegalStateException("Name of module must not be configured twice. "
                    + "Name in module-info.java: " + moduleNameFromModuleInfo.get() + " - "
                    + "Name in module.yml: " + moduleNameFromYml.get());
            }

            LOG.debug("Extracted module name {} from module-info", moduleNameFromModuleInfo.get());
            return moduleNameFromModuleInfo;
        }

        if (moduleNameFromYml.isPresent()) {
            LOG.debug("Extracted module name {} from module.yml", moduleNameFromYml.get());
            return moduleNameFromYml;
        }

        if (pathName != null) {
            LOG.debug("Extracted module name {} from module path", pathName);
            return Optional.of(pathName);
        }

        return Optional.empty();
    }

    private List<Module> scanForModules(final RuntimeConfigurationImpl rtConfig) {
        final List<Module> modules = new ArrayList<>();

        try {
            final List<Path> modulePaths = Files
                .list(LoomPaths.modulesDir(rtConfig.getProjectBaseDir()))
                .collect(Collectors.toList());

            for (final Path module : modulePaths) {
                final Path moduleBuildConfig = module.resolve("module.yml");

                final ModuleBuildConfig buildConfig;

                if (Files.exists(moduleBuildConfig)) {
                    final String modulePathName = module.getFileName().toString();
                    buildConfig = ConfigReader.readConfig(
                        rtConfig, moduleBuildConfig, modulePathName);
                } else {
                    buildConfig = new BuildConfigImpl();
                }

                final String moduleName = findModuleName(module, buildConfig,
                    module.getFileName().toString()).orElseThrow(() ->
                    new IllegalStateException("No module name could be determined"));

                modules.add(new Module(moduleName, module, buildConfig));
            }
            return modules;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkForInconsistentSrcModuleStruct(final Path projectBaseDir) {
        final boolean hasSrc = Files.exists(projectBaseDir.resolve("src"));
        final boolean hasModules = Files.exists(LoomPaths.modulesDir(projectBaseDir));

        if (hasSrc && hasModules) {
            throw new IllegalStateException("Directories src/ and modules/ are mutually exclusive");
        }
    }

    private Optional<String> readModuleNameFromModuleInfo(final Path baseDir) {
        final Path moduleInfoFile = baseDir.resolve(
            LoomPaths.SRC_MAIN.resolve(LoomPaths.MODULE_INFO_JAVA));

        final Path testModuleInfoFile = baseDir.resolve(
            LoomPaths.SRC_TEST.resolve(LoomPaths.MODULE_INFO_JAVA));

        if (Files.exists(testModuleInfoFile)) {
            if (Files.exists(moduleInfoFile)) {
                throw new IllegalStateException(
                    "module-info.java must not exist in both src/main and src/test");
            }
            if (Files.exists(baseDir.resolve(Paths.get("src", "main")))) {
                throw new IllegalStateException("No src/main must exist for integration test case");
            }

            // integration test module
            return readModuleNameFromInfoFile(testModuleInfoFile);
        }

        if (Files.exists(moduleInfoFile)) {
            return readModuleNameFromInfoFile(moduleInfoFile);
        }

        return Optional.empty();
    }

    private static Optional<String> readModuleNameFromInfoFile(final Path moduleInfoFile) {
        try {
            final String moduleInfoSource =
                new String(Files.readAllBytes(moduleInfoFile), StandardCharsets.UTF_8);
            final Pattern pattern = Pattern.compile("module\\s*(\\S+)\\s*\\{", Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(moduleInfoSource);

            if (!matcher.find()) {
                throw new IllegalStateException("Can't parse " + moduleInfoFile);
            }

            final String moduleName = matcher.group(1);
            return Optional.of(moduleName);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<ConfiguredTask> resolveTasks(final List<String> productIds) {
        return moduleRunner.resolveTasks(new HashSet<>(productIds));
    }

    public ExecutionReport execute(final List<ConfiguredTask> resolvedTasks)
        throws Exception {
        return moduleRunner.execute(resolvedTasks);
    }

    public void logSystemEnvironment() {
        LOG.debug("Running Loom {} on {} {} {}, Java {} ({}) with {} cores",
            LoomVersion.getVersion(),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            Runtime.version().toString(),
            System.getProperty("java.vendor"),
            Runtime.getRuntime().availableProcessors());

        LOG.debug("Used locale: {}", Locale.getDefault());
    }

    public void logMemoryUsage() {
        final Runtime rt = Runtime.getRuntime();
        final long maxMemory = rt.maxMemory();
        final long totalMemory = rt.totalMemory();
        final long freeMemory = rt.freeMemory();
        final long memUsed = totalMemory - freeMemory;

        LOG.debug("Memory max={}, total={}, free={}, used={}",
            maxMemory, totalMemory, freeMemory, memUsed);
    }

    public void generateDotProductOverview(final Path dotFile) {
        GraphvizOutput.generateDot(moduleRunner, dotFile);
    }

    public boolean isModuleBuild(final Path projectBaseDir) {
        checkForInconsistentSrcModuleStruct(projectBaseDir);
        return Files.exists(LoomPaths.modulesDir(projectBaseDir));
    }

}
