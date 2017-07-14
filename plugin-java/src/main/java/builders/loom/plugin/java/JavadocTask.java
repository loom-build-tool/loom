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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

import builders.loom.api.AbstractTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.ResourcesTreeProduct;
import builders.loom.api.product.SourceTreeProduct;

public class JavadocTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavadocTask.class);

    @Override
    public TaskResult run() throws Exception {
        final Optional<SourceTreeProduct> source = useProduct("source", SourceTreeProduct.class);

        if (!source.isPresent()) {
            return completeSkip();
        }

        final List<Path> classpath = new ArrayList<>();
        useProduct("compileDependencies", ClasspathProduct.class)
            .map(ClasspathProduct::getEntries)
            .ifPresent(classpath::addAll);

        final Path dstDir =
            Files.createDirectories(LoomPaths.BUILD_DIR.resolve(Paths.get("javadoc")));

        final DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();

        final DiagnosticListener<JavaFileObject> diagnosticListener =
            new DiagnosticLogListener(LOG);

        try (final StandardJavaFileManager fileManager = docTool.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT,
                Collections.singletonList(dstDir.toFile()));

            fileManager.setLocation(StandardLocation.CLASS_PATH,
                classpath.stream().map(Path::toFile).collect(Collectors.toList()));

            final List<File> srcFiles = source.get().getSourceFiles().stream()
                .map(Path::toFile)
                .collect(Collectors.toList());

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromFiles(srcFiles);

            LOG.info("Create Javadoc for {} files", srcFiles.size());

            final DocumentationTool.DocumentationTask javaDocTask =
                docTool.getTask(null, fileManager, diagnosticListener, null, null, compUnits);

            if (!javaDocTask.call()) {
                throw new IllegalStateException("JavaDoc compile failed");
            }
        }

        return completeOk(new ResourcesTreeProduct(dstDir));
    }

}
