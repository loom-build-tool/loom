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

package builders.loom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.config.ConfigReader;
import builders.loom.plugin.PluginLoader;
import builders.loom.util.FileUtils;
import builders.loom.util.Stopwatch;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
class LoomProcessor {

    private ModuleRunner moduleRunner;

    static {
        System.setProperty("loom.version", Version.getVersion());
    }

    public void init(final RuntimeConfigurationImpl runtimeConfiguration) {
        final Logger log = LoggerFactory.getLogger(LoomProcessor.class);

        final Stopwatch sw = new Stopwatch();

        // Init Modules / Plugins for Modules
        final ModuleRegistry moduleRegistry = new ModuleRegistry();
        listModules(runtimeConfiguration).forEach(moduleRegistry::register);

        log.debug("Initialized modules in {}", sw);

        final PluginLoader pluginLoader = new PluginLoader(runtimeConfiguration);
        moduleRunner = new ModuleRunner(runtimeConfiguration, pluginLoader, moduleRegistry);
        moduleRunner.init();
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
        final Path configFile = LoomPaths.PROJECT_DIR.resolve("module.yml");
        if (Files.notExists(configFile)) {
            throw new IllegalStateException("Missing module.yml in project root");
        }

        try {
            final ModuleBuildConfig buildConfig =
                ConfigReader.readConfig(rtConfig, configFile, "base");

            final String moduleName = findModuleName(LoomPaths.PROJECT_DIR, buildConfig, null);

            return new Module(moduleName, LoomPaths.PROJECT_DIR, buildConfig);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String findModuleName(final Path projectDir, final ModuleBuildConfig buildConfig,
                                  final String pathName) {

        final Optional<String> moduleName = readModuleNameFromModuleInfo(projectDir);
        final String automaticModuleName = buildConfig.getBuildSettings().getModuleName();

        if (moduleName.isPresent()) {
            if (automaticModuleName != null) {
                throw new IllegalStateException("Name of module must not be configured twice. "
                    + "Name in module-info.java: " + moduleName.get() + " - "
                    + "Name in module.yml: " + automaticModuleName);
            }

            return moduleName.get();
        }

        if (automaticModuleName != null) {
            return automaticModuleName;
        }

        if (pathName != null) {
            return pathName;
        }

        throw new IllegalStateException("Name of a module must be specified via "
            + "module-info.java "
            + "or setting automaticModuleName "
            + "or implicitly via modules subdirectory name");
    }

    private List<Module> scanForModules(final RuntimeConfigurationImpl runtimeConfiguration) {
        final List<Module> modules = new ArrayList<>();

        try {
            final List<Path> modulePaths = Files.list(LoomPaths.MODULES_DIR)
                .collect(Collectors.toList());

            for (final Path module : modulePaths) {
                final Path moduleBuildConfig = module.resolve("module.yml");
                if (Files.notExists(moduleBuildConfig)) {
                    throw new IllegalStateException("Missing module.yml in module " + module);
                }

                final String modulePathName = module.getFileName().toString();
                final ModuleBuildConfig buildConfig = ConfigReader.readConfig(
                    runtimeConfiguration, moduleBuildConfig, modulePathName);

                final String moduleName = findModuleName(module, buildConfig,
                    module.getFileName().toString());

                modules.add(new Module(moduleName, module, buildConfig));
            }
            return modules;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkForInconsistentSrcModuleStruct() {
        final boolean hasSrc = Files.exists(LoomPaths.PROJECT_DIR.resolve("src"));
        final boolean hasModules = Files.exists(LoomPaths.MODULES_DIR);

        if (hasSrc && hasModules) {
            throw new IllegalStateException("Directories src/ and modules/ are mutually exclusive");
        }
    }

    private Optional<String> readModuleNameFromModuleInfo(final Path baseDir) {
        final Path moduleInfoFile = baseDir.resolve(
            LoomPaths.SRC_MAIN.resolve("module-info.java"));

        final Path testModuleInfoFile = baseDir.resolve(
            LoomPaths.SRC_TEST.resolve("module-info.java"));

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

    public Optional<ExecutionReport> execute(final List<String> productIds) throws Exception {
        return moduleRunner.execute(new HashSet<>(productIds));
    }

    public void clean() {
        FileUtils.cleanDir(LoomPaths.BUILD_DIR);
        FileUtils.cleanDir(LoomPaths.PROJECT_LOOM_PATH);
    }

    public void logSystemEnvironment() {
        final Logger log = LoggerFactory.getLogger(LoomProcessor.class);
        log.debug("Running Loom {} on {} {} {}, Java {} ({}) with {} cores",
            Version.getVersion(),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            Runtime.version().toString(),
            System.getProperty("java.vendor"),
            Runtime.getRuntime().availableProcessors());
    }

    public void logMemoryUsage() {
        final Logger log = LoggerFactory.getLogger(LoomProcessor.class);

        final Runtime rt = Runtime.getRuntime();
        final long maxMemory = rt.maxMemory();
        final long totalMemory = rt.totalMemory();
        final long freeMemory = rt.freeMemory();
        final long memUsed = totalMemory - freeMemory;

        log.debug("Memory max={}, total={}, free={}, used={}",
            maxMemory, totalMemory, freeMemory, memUsed);
    }

    public void generateTextProductOverview() {
        TextOutput.generate(moduleRunner);
    }

    public void generateDotProductOverview() {
        GraphvizOutput.generateDot(moduleRunner);
    }

    public boolean isModuleBuild() {
        checkForInconsistentSrcModuleStruct();
        return Files.exists(LoomPaths.MODULES_DIR);
    }

}
