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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarOutputStream;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.DirectoryProduct;

public class JavaAssembleJavadocJarTask extends AbstractModuleTask {

    @Override
    public TaskResult run() throws Exception {
        final Optional<DirectoryProduct> resourcesTreeProduct = useProduct(
            "javadoc", DirectoryProduct.class);

        if (!resourcesTreeProduct.isPresent()) {
            return completeEmpty();
        }

        final Path buildDir = Files.createDirectories(LoomPaths.buildDir(
            getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(),
            "javadoc-jar"));

        final Path jarFile = buildDir.resolve(String.format("%s-javadoc.jar",
            getBuildContext().getModuleName()));

        try (final JarOutputStream os = new JarOutputStream(Files.newOutputStream(jarFile))) {
            FileUtil.copy(resourcesTreeProduct.get().getDir(), os);
        }

        return completeOk(new AssemblyProduct(jarFile, "Jar of Javadoc"));
    }

}
