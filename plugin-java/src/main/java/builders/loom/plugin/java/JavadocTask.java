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

import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.DirectoryProduct;
import builders.loom.api.product.SourceTreeProduct;

public class JavadocTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavadocTask.class);

    @Override
    public TaskResult run() throws Exception {
        final Optional<SourceTreeProduct> source = useProduct("source", SourceTreeProduct.class);

        if (!source.isPresent()) {
            return completeEmpty();
        }

        final Path dstDir = Files.createDirectories(
            LoomPaths.buildDir(getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(), "javadoc"));

        final DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();

        final DiagnosticListener<JavaFileObject> diagnosticListener =
            new DiagnosticLogListener(LOG);

        try (final StandardJavaFileManager fileManager = docTool.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT,
                List.of(dstDir));

            final Optional<ClasspathProduct> compileDependencies =
                useProduct("compileDependencies", ClasspathProduct.class);

            if (compileDependencies.isPresent()) {
                fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH,
                    compileDependencies.get().getEntries());
            }

            for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
                // TODO doesn't work
/*
                final Optional<CompilationProduct> compilation =
                    useProduct(moduleName, "compilation", CompilationProduct.class);

                if (compilation.isPresent()) {
                    fileManager.setLocationForModule(StandardLocation.MODULE_PATH,
                        moduleName, List.of(compilation.get().getClassesDir()));
                }
*/

                // workaround - step 1/2
                getUsedProducts().getAndWaitProduct(moduleName, "compilation");
            }

            // workaround - step 2/2
            fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, List.of(getBuildDir().getParent()));

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromPaths(source.get().getSourceFiles());

            LOG.info("Create Javadoc for {} files", source.get().getSourceFiles().size());

            final DocumentationTool.DocumentationTask javaDocTask =
                docTool.getTask(null, fileManager, diagnosticListener, null, null, compUnits);

            if (!javaDocTask.call()) {
                throw new IllegalStateException("JavaDoc compile failed");
            }
        }

        return completeOk(new DirectoryProduct(dstDir, "JavaDoc output"));
    }

    private Path getBuildDir() {
        // TODO another workaround for non-functional MODULE_PATH
        return LoomPaths.buildDir(getRuntimeConfiguration().getProjectBaseDir())
            .resolve(Paths.get("compilation", CompileTarget.MAIN.name().toLowerCase(),
                getBuildContext().getModuleName()));
    }

}
