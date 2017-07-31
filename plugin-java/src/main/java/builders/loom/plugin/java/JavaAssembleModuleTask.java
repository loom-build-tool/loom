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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import jdk.internal.module.ModuleInfoExtender;

public class JavaAssembleModuleTask extends AbstractModuleTask {

    private final JavaPluginSettings pluginSettings;

    public JavaAssembleModuleTask(final JavaPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<ProcessedResourceProduct> resourcesTreeProduct = useProduct(
            "processedResources", ProcessedResourceProduct.class);

        final Optional<CompilationProduct> compilation = useProduct(
            "compilation", CompilationProduct.class);

        if (!resourcesTreeProduct.isPresent() && !compilation.isPresent()) {
            return completeSkip();
        }

        final Path buildDir = Files.createDirectories(
            LoomPaths.buildDir(getBuildContext().getModuleName(), "jar"));

        final Path jarFile = buildDir.resolve(String.format("%s.jar",
            getBuildContext().getModuleName()));

        try (final JarOutputStream os = buildJarOutput(jarFile)) {
            // compilation & module-info.class first !
            if (compilation.isPresent()) {
                final Path resolve = compilation.get().getClassesDir();

                // TODO cleanup
                // TODO move module-info.class related stuff to LoomPaths
                final Path modulesInfoClassFile = resolve.resolve("module-info.class");
                if (Files.exists(modulesInfoClassFile)) {
                    final JarEntry entry =
                        new JarEntry(resolve.relativize(modulesInfoClassFile).toString());
                    entry.setTime(Files.getLastModifiedTime(modulesInfoClassFile).toMillis());
                    os.putNextEntry(entry);
                    os.write(extendedModuleInfoClass(modulesInfoClassFile));
                    os.closeEntry();
                }

                FileUtil.copy(resolve, os);
            }

            if (resourcesTreeProduct.isPresent()) {
                FileUtil.copy(resourcesTreeProduct.get().getSrcDir(), os);
            }
        }

        return completeOk(new AssemblyProduct(jarFile, "Jar of compiled classes"));
    }

    private byte[] extendedModuleInfoClass(final Path modulesInfoClassFile) {

        // TODO replace JDK ModuleInfoExtender by something similar, then remove
        // --add-exports=java.base/jdk.internal.module=ALL-UNNAMED from build and loom scripts

        try (InputStream classIs = Files.newInputStream(modulesInfoClassFile)) {
            final ModuleInfoExtender extender = ModuleInfoExtender.newExtender(classIs);

            Optional.ofNullable(pluginSettings.getMainClassName())
                .ifPresent(extender::mainClass);

            return extender.toByteArray();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JarOutputStream buildJarOutput(final Path jarFile) throws IOException {
        return new JarOutputStream(Files.newOutputStream(jarFile), prepareManifest());
    }

    private Manifest prepareManifest() {
        final Manifest newManifest = new Manifest();

        final ManifestBuilder manifestBuilder = new ManifestBuilder(newManifest);

        manifestBuilder
            .put(Attributes.Name.MANIFEST_VERSION, "1.0")
            .put("Created-By", "Loom " + System.getProperty("loom.version"))
            .put("Build-Jdk", String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        Optional.ofNullable(pluginSettings.getMainClassName())
            .ifPresent(s -> manifestBuilder.put(Attributes.Name.MAIN_CLASS, s));

        return newManifest;
    }

}
