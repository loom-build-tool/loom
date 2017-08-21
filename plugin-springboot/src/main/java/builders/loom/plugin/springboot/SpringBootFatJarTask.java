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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.DirectoryProduct;

public class SpringBootFatJarTask extends AbstractModuleTask {

    private static final String SPRING_BOOT_APPLICATION_ANNOTATION =
        "org.springframework.boot.autoconfigure.SpringBootApplication";

    private final SpringBootPluginSettings pluginSettings;

    public SpringBootFatJarTask(final SpringBootPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskResult run() throws Exception {
        final DirectoryProduct springBootApplication =
            requireProduct("springBootApplication", DirectoryProduct.class);

        final Path baseDir = springBootApplication.getDir();
        final Path buildDir = Files.createDirectories(baseDir.getParent().resolve("fatjar"));

        final Path jarFile = buildDir.resolve(String.format("%s-fatjar.jar",
            getBuildContext().getModuleName()));

        // scan for @SpringBootApplication
        final CompilationProduct compilationProduct =
            requireProduct("compilation", CompilationProduct.class);
        final String applicationClassname = scanForApplicationStarter(compilationProduct);

        // assemble jar
        new JarAssembler(pluginSettings).assemble(baseDir, jarFile, applicationClassname);

        return completeOk(new AssemblyProduct(jarFile, "Spring Boot Fat Jar application"));
    }

    private String scanForApplicationStarter(final CompilationProduct compilationProduct)
        throws IOException {

        final String applicationClassname = new ClassScanner()
            .scanArchives(compilationProduct.getClassesDir(), SPRING_BOOT_APPLICATION_ANNOTATION);

        if (applicationClassname == null) {
            throw new IllegalStateException("Couldn't find class with "
                + SPRING_BOOT_APPLICATION_ANNOTATION + " annotation");
        }

        return applicationClassname;
    }

}
