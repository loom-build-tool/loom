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

package builders.loom.plugin.springboot;

import java.nio.file.Path;
import java.nio.file.Paths;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ManagedGenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class SpringBootFatJarTask extends AbstractModuleTask {

    @Override
    public TaskResult run() throws Exception {
        final Product springBootApplication =
            requireProduct("springBootApplication", Product.class);

        final Path baseDir = Paths.get(springBootApplication.getProperty("springBootOut"));
        final Path buildDir = FileUtil.createOrCleanDirectory(resolveBuildDir("springboot-fatjar"));

        final Path jarFile = buildDir.resolve(String.format("%s-fatjar.jar",
            getBuildContext().getModuleName()));

        // assemble jar
        new JarAssembler().assemble(baseDir, jarFile);

        return TaskResult.done(newProduct(jarFile));
    }

    private static Product newProduct(final Path jarFile) {
        return new ManagedGenericProduct("springBootFatJar", jarFile.toString(),
            ProductChecksumUtil.recursiveMetaChecksum(jarFile),
            new OutputInfo("Spring Boot Fat Jar application", jarFile));
    }

}
