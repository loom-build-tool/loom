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

package builders.loom.plugin.java;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.util.TempFile;

public class JavaAssembleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavaAssembleTask.class);

    private final JavaPluginSettings pluginSettings;

    public JavaAssembleTask(final JavaPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    @Override
    public TaskResult run() throws Exception {
        final Optional<CompilationProduct> compilationProduct = useProduct(
            "compilation", CompilationProduct.class);

        final Optional<ProcessedResourceProduct> resourcesTreeProduct = useProduct(
            "processedResources", ProcessedResourceProduct.class);

        if (!compilationProduct.isPresent() && !resourcesTreeProduct.isPresent()) {
            return completeEmpty();
        }

        final Path jarFile = Files
            .createDirectories(resolveBuildDir("jar"))
            .resolve(String.format("%s.jar", getBuildContext().getModuleName()));

        final ToolProvider toolProvider = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new IllegalStateException("Couldn't find jar ToolProvider"));

        final List<String> args = new ArrayList<>(List.of("-c", "-f", jarFile.toString()));

        if (pluginSettings.getMainClassName() != null) {
            args.addAll(List.of("-e", pluginSettings.getMainClassName()));
        }

        final int result;

        final String automaticModuleName =
            buildAutomaticModuleName(compilationProduct.orElse(null));
        final Manifest manifest = prepareManifest(automaticModuleName);

        final Path tmpDir = Files.createDirectories(LoomPaths.tmpDir(getBuildContext().getPath()));
        try (TempFile tf = new TempFile(tmpDir, "meta-inf", null)) {
            final Path file = tf.getFile();
            try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
                manifest.write(os);
            }

            args.addAll(List.of("-m", file.toString()));

            compilationProduct.ifPresent(p ->
                args.addAll(List.of("-C", p.getClassesDir().toString(), ".")));

            resourcesTreeProduct.ifPresent(p ->
                args.addAll(List.of("-C", p.getSrcDir().toString(), ".")));

            LOG.debug("Run JarToolProvider with args: {}", args);
            result = toolProvider.run(System.out, System.err, args.toArray(new String[]{}));
        }

        if (result != 0) {
            throw new IllegalStateException("Building jar file failed");
        }

        return completeOk(new AssemblyProduct(jarFile, "Jar of compiled classes"));
    }

    private String buildAutomaticModuleName(final CompilationProduct compilationProduct) {
        if (compilationProduct != null) {
            final Path moduleInfo = compilationProduct.getClassesDir()
                .resolve(LoomPaths.MODULE_INFO_CLASS);

            if (Files.exists(moduleInfo)) {
                // Automatic-Module-Name not required -- got module-info.class
                return null;
            }
        }

        if (!Module.UNNAMED_MODULE.equals(getBuildContext().getModuleName())) {
            // Use configured module name
            return getBuildContext().getModuleName();
        }

        // Unnamed module -- better use default name
        LOG.warn("Building jar file without module name is discouraged! Configure a "
            + "module name by either adding a module-info.java, using the module/ "
            + "directory structure or add moduleName to module.yml");
        return null;
    }

    private Manifest prepareManifest(final String automaticModuleName) {
        final Manifest newManifest = new Manifest();

        final ManifestBuilder manifestBuilder = new ManifestBuilder(newManifest);

        manifestBuilder
            .put(Attributes.Name.MANIFEST_VERSION, "1.0")
            .put("Created-By", "Loom " + System.getProperty("loom.version"))
            .put("Build-Jdk", String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        Optional.ofNullable(automaticModuleName)
            .ifPresent(s -> manifestBuilder.put("Automatic-Module-Name", s));

        return newManifest;
    }

}
