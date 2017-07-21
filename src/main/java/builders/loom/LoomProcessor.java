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

import builders.loom.ModuleRunner.ConfiguredModuleTask;
import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.product.Product;
import builders.loom.config.ConfigReader;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.util.FileUtils;
import builders.loom.util.Stopwatch;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class LoomProcessor {

    private final ModuleRegistry moduleRegistry = new ModuleRegistry();
    private ModuleRunner moduleRunner;

    static {
        System.setProperty("loom.version", Version.getVersion());
    }

    public void init(final BuildConfigWithSettings buildConfig,
                     final RuntimeConfigurationImpl runtimeConfiguration) {

        Stopwatch.startProcess("Initialize module configurations");
        listModules(buildConfig, runtimeConfiguration)
        		.forEach(moduleRegistry::register);
        Stopwatch.stopProcess();


        moduleRunner = new ModuleRunner(moduleRegistry,
            buildConfig, runtimeConfiguration);
        moduleRunner.init();

    }

    public List<Module> listModules(final BuildConfigWithSettings buildConfig, final RuntimeConfigurationImpl runtimeConfiguration) {
    	
    		checkForInconsistentSrcModuleStruct();
    		
    		final Path modulesPath = LoomPaths.PROJECT_DIR.resolve("modules");
    		
    		if (Files.isDirectory(modulesPath)) {
    			return scanForModules(runtimeConfiguration);
    		}

    		return singleModule(buildConfig, runtimeConfiguration);
    }
    
    private List<Module> singleModule(final BuildConfigWithSettings buildConfig, final RuntimeConfigurationImpl runtimeConfiguration) {
    	
	    	final Path moduleBuildConfig = LoomPaths.BUILD_FILE;
        if (Files.notExists(moduleBuildConfig)) {
            throw new IllegalStateException("Missing build.yml in project root");
        }
        
        final String moduleName = readModuleNameFromModuleInfo(LoomPaths.PROJECT_DIR)
        		.orElse("unnamed");

        return List.of(new Module(moduleName, moduleName, LoomPaths.PROJECT_DIR, buildConfig));
	}

	public List<Module> scanForModules(final RuntimeConfigurationImpl runtimeConfiguration) {
		
    		final List<Module> modules = new ArrayList<>();
    	
    		final Path modulesPath = LoomPaths.PROJECT_DIR.resolve("modules");
        try {
			final List<Path> modulePaths = Files.list(modulesPath)
                .collect(Collectors.toList());

            for (final Path module : modulePaths) {
                final Path moduleBuildConfig = module.resolve("build.yml");
                if (Files.notExists(moduleBuildConfig)) {
                    throw new IllegalStateException("Missing build.yml in module " + module);
                }

                final String modulePathName = module.getFileName().toString();
                final BuildConfigWithSettings buildConfig = ConfigReader.readConfig(
                    runtimeConfiguration, moduleBuildConfig, modulePathName);

                // TODO src/test/java ?

                final String moduleName = readModuleNameFromModuleInfo(module)
                		.orElseThrow(() -> new IllegalStateException(
                				"Missing module-info.java in module " + module));

                modules.add(new Module(modulePathName, moduleName, module, buildConfig));
            }
            return modules;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	private void checkForInconsistentSrcModuleStruct() {
		final boolean hasSrc = Files.exists(LoomPaths.PROJECT_DIR.resolve("src"));
		final boolean hasModules = Files.exists(LoomPaths.PROJECT_DIR.resolve("modules"));
		
		if (hasSrc && hasModules) {
			throw new IllegalStateException("Directories src/ and modules/ are mutually exclusive");
		}
	}

	private Optional<String> readModuleNameFromModuleInfo(final Path baseDir) {

		final Path moduleInfoFile = baseDir.resolve(
				Paths.get("src", "main", "java", "module-info.java"));

		if (Files.notExists(moduleInfoFile)) {
			return Optional.empty();
		}

		try {
			final String moduleInfoSource = new String(Files.readAllBytes(moduleInfoFile), StandardCharsets.UTF_8);
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

    public List<ConfiguredModuleTask> execute(final List<String> productIds) throws Exception {
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
            System.getProperty("java.version"),
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

    public void printProductInfos(final Collection<ConfiguredModuleTask> resolvedTasks)
        throws InterruptedException {

        // aggregate plugin -> products
        final Map<String, List<ProductInfo>> aggProducts = aggregateProducts(resolvedTasks);

        if (!aggProducts.isEmpty()) {
            outputProductInfos(aggProducts);
        }
    }

    private Map<String, List<ProductInfo>> aggregateProducts(
        final Collection<ConfiguredModuleTask> resolvedTasks) throws InterruptedException {
    	
        // plugin -> products
        final Map<String, List<ProductInfo>> aggProducts = new HashMap<>();

        for (final ConfiguredModuleTask configuredModuleTask : resolvedTasks) {
        	final ConfiguredTask configuredTask = configuredModuleTask.getConfiguredTask();
        		final String productId = configuredTask.getProvidedProduct();
        		
            final Optional<Product> product = moduleRunner.lookupProduct(configuredModuleTask.getModule(), productId)
                .getAndWaitForProduct();
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
