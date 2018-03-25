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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ManagedGenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class JavadocTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavadocTask.class);

    @Override
    public TaskResult run() throws Exception {
        final Optional<Product> source = useProduct("source", Product.class);

        if (!source.isPresent()) {
            return TaskResult.empty();
        }

        final Path buildDir = FileUtil.createOrCleanDirectory(resolveBuildDir("javadoc"));

        final DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();

        final DiagnosticListener<JavaFileObject> diagnosticListener =
            new DiagnosticLogListener(LOG);

        try (final StandardJavaFileManager fileManager = docTool.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT,
                List.of(buildDir));

            final Optional<Product> compileDependencies =
                useProduct("compileDependencies", Product.class);

            if (compileDependencies.isPresent()) {
                final List<Path> files = compileDependencies.get()
                    .getProperties("classpath").stream()
                    .map(p -> Paths.get(p)).collect(Collectors.toList());

                fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, files);
            }

            for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
                final Optional<Path> moduleBuildPath =
                    useProduct(moduleName, "compilation", Product.class)
                        .map(p -> Paths.get(p.getProperty("classesDir")));

                if (moduleBuildPath.isPresent()) {
                    fileManager.setLocationForModule(StandardLocation.MODULE_PATH,
                        moduleName, List.of(moduleBuildPath.get()));
                }
            }

            final Path srcDir = Paths.get(source.get().getProperty("srcDir"));
            final List<Path> srcFiles = Files
                .find(srcDir, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())
                .collect(Collectors.toList());

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromPaths(srcFiles);

            LOG.info("Create Javadoc for {} files", srcFiles.size());

            final DocumentationTool.DocumentationTask javaDocTask =
                docTool.getTask(null, fileManager, diagnosticListener, null, null, compUnits);

            if (!javaDocTask.call()) {
                throw new IllegalStateException("JavaDoc compile failed");
            }
        }

        return TaskResult.done(newProduct(buildDir));
    }

    private static Product newProduct(final Path buildDir) {
        return new ManagedGenericProduct("javaDocOut", buildDir.toString(),
            ProductChecksumUtil.recursiveMetaChecksum(buildDir),
            new OutputInfo("JavaDoc output", buildDir));
    }

}
