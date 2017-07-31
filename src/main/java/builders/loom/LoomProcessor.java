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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.api.product.Product;
import builders.loom.config.BuildConfigImpl;
import builders.loom.config.ConfigReader;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.PluginLoader;
import builders.loom.util.FileUtils;
import builders.loom.util.Stopwatch;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class LoomProcessor {

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

        log.debug("Initialized modules in {} ms", sw.duration());

        final PluginLoader pluginLoader = new PluginLoader(runtimeConfiguration);
        moduleRunner = new ModuleRunner(runtimeConfiguration, pluginLoader, moduleRegistry);
        moduleRunner.init();
    }

    public List<Module> listModules(final RuntimeConfigurationImpl runtimeConfiguration) {
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

        final String moduleName = readModuleNameFromModuleInfo(LoomPaths.PROJECT_DIR)
            .orElse("unnamed");

        final ModuleBuildConfig buildConfig = readConfig(rtConfig, configFile)
            .orElseGet(BuildConfigImpl::new);

        return new Module(moduleName, LoomPaths.PROJECT_DIR, buildConfig);
    }

    private static Optional<ModuleBuildConfig> readConfig(
        final RuntimeConfigurationImpl rtConfig, final Path buildFile) {

        if (Files.notExists(buildFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(ConfigReader.readConfig(rtConfig, buildFile, "base"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Module> scanForModules(final RuntimeConfigurationImpl runtimeConfiguration) {

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

                final String moduleName = readModuleNameFromModuleInfo(module)
                    .orElse(module.getFileName().toString());

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
            Paths.get("src", "main", "java", "module-info.java"));

        final Path testModuleInfoFile = baseDir.resolve(
            Paths.get("src", "test", "java", "module-info.java"));

        if (Files.exists(testModuleInfoFile)) {
            if (Files.exists(moduleInfoFile)) {
                throw new IllegalStateException(
                    "module-info.java must not exist in both src/main and src/test");
            }
            if (Files.exists(baseDir.resolve(Paths.get("src", "main")))) {
                throw new IllegalStateException("No src/main must exist for itest case");
            }

            // sicher ein itest
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

    public void printProductInfos(final Collection<ConfiguredTask> resolvedTasks)
        throws InterruptedException {

        // aggregate plugin -> products
        final Map<String, List<ProductInfo>> aggProducts = aggregateProducts(resolvedTasks);

        if (!aggProducts.isEmpty()) {
            outputProductInfos(aggProducts);
        }
    }

    private Map<String, List<ProductInfo>> aggregateProducts(
        final Collection<ConfiguredTask> resolvedTasks) {

        // plugin -> products
        final Map<String, List<ProductInfo>> aggProducts = new HashMap<>();

        for (final ConfiguredTask configuredTask : resolvedTasks) {
            final String productId = configuredTask.getProvidedProduct();

            final Optional<Product> product = moduleRunner
                .lookupProduct(configuredTask.getBuildContext(), productId)
                .getWithoutWait();

            if (product.isPresent() && product.get().outputInfo().isPresent()) {
                final String outputInfo = product.get().outputInfo().get();
                final String pluginName = configuredTask.getPluginName();
                aggProducts.putIfAbsent(pluginName, new ArrayList<>());
                aggProducts.get(pluginName).add(new ProductInfo(productId, outputInfo));
            }
        }
        return aggProducts;
    }

    private void outputProductInfos(final Map<String, List<ProductInfo>> aggProducts) {
        AnsiConsole.out().println();

        final List<String> pluginNames = new ArrayList<>(aggProducts.keySet());
        Collections.sort(pluginNames);

        for (final Iterator<String> iterator = pluginNames.iterator(); iterator.hasNext();) {
            final String pluginName = iterator.next();

            final List<ProductInfo> productInfos = aggProducts.get(pluginName);

            final Ansi ansi = Ansi.ansi()
                .a("Products of ")
                .bold()
                .a(pluginName)
                .reset()
                .a(":")
                .newline();

            for (final ProductInfo productInfo : productInfos) {
                ansi
                    .fgBrightYellow()
                    .a("> ")
                    .fgBrightGreen()
                    .a(productInfo.getOutputInfo())
                    .reset()
                    .fgBlack().bold()
                    .format(" [%s]", productInfo.getProductId())
                    .newline();
            }

            ansi.reset();

            if (iterator.hasNext()) {
                ansi.newline();
            }

            AnsiConsole.out().print(ansi);
        }
    }

    public boolean isModuleBuild() {
        checkForInconsistentSrcModuleStruct();
        return Files.exists(LoomPaths.MODULES_DIR);
    }

    private static final class ProductInfo {

        private final String productId;
        private final String outputInfo;

        ProductInfo(final String productId, final String outputInfo) {
            this.productId = productId;
            this.outputInfo = outputInfo;
        }

        public String getProductId() {
            return productId;
        }

        public String getOutputInfo() {
            return outputInfo;
        }

    }

}
