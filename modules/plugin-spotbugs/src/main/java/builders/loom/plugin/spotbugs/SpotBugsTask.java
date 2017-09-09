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

package builders.loom.plugin.spotbugs;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;
import builders.loom.util.FileUtil;
import builders.loom.util.StringUtil;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;

@SuppressWarnings("checkstyle:classdataabstractioncoupling")
public class SpotBugsTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(SpotBugsTask.class);

    private final CompileTarget compileTarget;
    private final SpotBugsPluginSettings pluginSettings;
    private final String effort;
    private final List<String> plugins;
    private final int priorityThreshold;
    private final String sourceProductId;
    private final String compilationProductId;
    private final String reportOutputDescription;
    private final ReporterType reporter;

    public SpotBugsTask(final CompileTarget compileTarget,
                        final SpotBugsPluginSettings pluginSettings) {

        this.compileTarget = Objects.requireNonNull(compileTarget);
        this.pluginSettings = pluginSettings;
        effort = pluginSettings.getEffort();
        priorityThreshold = SpotBugsUtil.resolvePriority(pluginSettings.getReportLevel());
        plugins = StringUtil.split(pluginSettings.getCustomPlugins(), ",");
        reporter = ReporterType.valueOf(pluginSettings.getReporter().toUpperCase());

        switch (compileTarget) {
            case MAIN:
                sourceProductId = "source";
                compilationProductId = "compilation";
                reportOutputDescription = "SpotBugs main report";
                break;
            case TEST:
                sourceProductId = "testSource";
                compilationProductId = "testCompilation";
                reportOutputDescription = "SpotBugs test report";
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public TaskResult run() throws Exception {
        final List<String> classFiles =
            useProduct(compilationProductId, CompilationProduct.class)
                .map(CompilationProduct::getClassesDir)
                .map(SpotBugsTask::getClassesToScan)
                .orElse(Collections.emptyList());

        if (classFiles.isEmpty()) {
            return completeEmpty();
        }

        final List<Path> srcFiles =
            useProduct(sourceProductId, SourceTreeProduct.class)
            .map(SourceTreeProduct::getSrcFiles)
            .orElse(Collections.emptyList());

        final Path reportDir =
            FileUtil.createOrCleanDirectory(resolveReportDir("spotbugs", compileTarget));

        SpotBugsSingleton.initSpotBugs(plugins);

        final Project project = createSpotBugsProject(srcFiles, classFiles, calcClasspath());
        executeSpotBugs(project, reportDir);

        return completeOk(new ReportProduct(reportDir, reportOutputDescription));
    }

    private static List<String> getClassesToScan(final Path classesDir) {
        try {
            return Files
                .find(classesDir, Integer.MAX_VALUE, (path, attr) -> filterClassFiles(attr, path))
                .map(file -> file.toAbsolutePath().normalize().toString())
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean filterClassFiles(final BasicFileAttributes attr, final Path path) {
        if (!attr.isRegularFile()) {
            return false;
        }

        final String fileName = path.getFileName().toString();

        if (LoomPaths.MODULE_INFO_CLASS.equals(fileName)) {
            // remove as soon as SpotBugs supports module-info.class
            return false;
        }

        return fileName.endsWith(".class");
    }

    private List<Path> calcClasspath() throws InterruptedException {
        final List<Path> classpath = new ArrayList<>();

        switch (compileTarget) {
            case MAIN:
                useProduct("compileDependencies", ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            case TEST:
                useProduct("compilation", CompilationProduct.class)
                    .map(CompilationProduct::getClassesDir)
                    .ifPresent(classpath::add);

                useProduct("testDependencies", ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            default:
                throw new IllegalArgumentException("Unknown target: " + compileTarget);
        }
        return classpath;
    }

    private Project createSpotBugsProject(final List<Path> sourceFiles,
                                          final List<String> classes,
                                          final List<Path> classpath) {

        final Project spotBugsProject = new Project();

        LOG.debug("Add sources: {}", sourceFiles);
        sourceFiles.stream()
            .map(Path::toString)
            .forEach(spotBugsProject::addFile);

        LOG.debug("Add classes: {}", classes);
        classes.forEach(spotBugsProject::addFile);

        LOG.debug("Add classpath: {}", classpath);
        classpath.stream()
            .map(file -> file.toAbsolutePath().normalize().toString())
            .forEach(spotBugsProject::addAuxClasspathEntry);

        return spotBugsProject;
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    private void executeSpotBugs(final Project project, final Path reportDir)
        throws IOException, InterruptedException {

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        final BugReporter bugReporter = setupBugReporter(project, reportDir);

        try {
            final FindBugs2 engine = new FindBugs2();
            engine.setProject(project);
            engine.setBugReporter(bugReporter);
            engine.setNoClassOk(false);
            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

            final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
            userPreferences.setEffort(effort);

            Optional.ofNullable(pluginSettings.getIncludeFilterFiles())
                .ifPresent(files -> userPreferences.getIncludeFilterFiles().put(files, true));

            Optional.ofNullable(pluginSettings.getExcludeFilterFiles())
                .ifPresent(files -> userPreferences.getExcludeFilterFiles().put(files, true));

            engine.setUserPreferences(userPreferences);

            // this is required, because edu.umd.cs.findbugs.FindBugs2.setUserPreferences()
            // does not. See comment there...
            engine.setAnalysisFeatureSettings(userPreferences.getAnalysisFeatureSettings());

            engine.finishSettings();

            engine.execute();

            if (engine.getBugCount() + engine.getErrorCount() > 0) {
                throw new IllegalStateException(String.format(
                    "SpotBugs reported %d bugs and %d errors",
                    engine.getBugCount(), engine.getErrorCount()));
            }
        } finally {
            bugReporter.finish();
            System.setSecurityManager(currentSecurityManager);
        }
    }

    private BugReporter setupBugReporter(final Project project, final Path reportDir)
        throws IOException {

        if (reporter == ReporterType.HTML) {
            final LogHTMLBugReporter htmlBugReporter =
                new LogHTMLBugReporter(project, "default.xsl");
            htmlBugReporter.setPriorityThreshold(priorityThreshold);
            htmlBugReporter.setOutputStream(new PrintStream(
                reportDir.resolve("spotbugs-result.html").toFile(), "UTF-8"));
            return htmlBugReporter;
        }

        if (reporter == ReporterType.XML) {
            final LogXMLBugReporter xmlBugReporter = new LogXMLBugReporter(project);
            xmlBugReporter.setPriorityThreshold(priorityThreshold);
            xmlBugReporter.setAddMessages(true);
            xmlBugReporter.setOutputStream(new PrintStream(
                reportDir.resolve("spotbugs-result.xml").toFile(), "UTF-8"));
            return xmlBugReporter;
        }

        throw new IllegalStateException("Unknown reporter: " + reporter);
    }

}
