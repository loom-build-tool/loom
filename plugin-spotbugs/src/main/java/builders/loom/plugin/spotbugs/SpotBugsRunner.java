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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.util.SystemUtil;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.HTMLBugReporter;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class SpotBugsRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SpotBugsRunner.class);

    private static final String EFFORT_DEFAULT = "default";

    private final List<Path> sourceFiles;
    private final Path classesDir;
    private final List<Path> classpath;

    private final int priorityThreshold;
    private final Path reportPath;

    SpotBugsRunner(final Path reportPath, final List<Path> sourceFiles, final Path classesDir,
                   final List<Path> classpath, final int priorityThreshold) {
        this.reportPath = reportPath;
        this.sourceFiles = sourceFiles;
        this.classesDir = classesDir;
        this.classpath = classpath;
        this.priorityThreshold = priorityThreshold;
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public void executeSpotBugs() throws IOException, InterruptedException {
        prepareEnvironment();

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        final Project project = createSpotBugsProject();
        if (project.getFileCount() == 0) {
            LOG.info("SpotBugs analysis skipped for this project.");
            return;
        }

        final LoggingBugReporter loggingBugReporter = new LoggingBugReporter();
        loggingBugReporter.setPriorityThreshold(priorityThreshold);

        final XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
        xmlBugReporter.setPriorityThreshold(priorityThreshold);
        xmlBugReporter.setAddMessages(true);
        xmlBugReporter.setOutputStream(new PrintStream(
            reportPath.resolve("spotbugs-result.xml").toFile(), "UTF-8"));

        final HTMLBugReporter htmlBugReporter = new HTMLBugReporter(project, "default.xsl");
        htmlBugReporter.setPriorityThreshold(priorityThreshold);
        htmlBugReporter.setOutputStream(new PrintStream(
            reportPath.resolve("spotbugs-result.html").toFile(), "UTF-8"));

        final MultiplexingBugReporter multiplexingBugReporter =
            new MultiplexingBugReporter(loggingBugReporter, xmlBugReporter, htmlBugReporter);

        try {
            final FindBugs2 engine = new FindBugs2();
            engine.setProject(project);
            engine.setBugReporter(multiplexingBugReporter);
            engine.setNoClassOk(true);
            engine.setUserPreferences(buildUserPreferences());
            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);
            engine.finishSettings();

            engine.execute();

            if (engine.getErrorCount() + engine.getBugCount() > 0) {
                throw new IllegalStateException("SpotBugs reported bugs!");
            }
        } finally {
            multiplexingBugReporter.finish();
            System.setSecurityManager(currentSecurityManager);
            Locale.setDefault(initialLocale);
        }
    }

    private void prepareEnvironment() {
        LOG.debug("Prepare/cleanup SpotBugs environment...");
        try {
            Files.deleteIfExists(reportPath.resolve("spotbugs-result.xml"));
            Files.createDirectories(reportPath);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        LOG.debug("...cleanup done");
    }

    private Project createSpotBugsProject() throws IOException {
        final Project spotBugsProject = new Project();

        sourceFiles.stream()
            .map(Path::toString)
            .peek(p -> LOG.debug(" +source {}", p))
            .forEach(spotBugsProject::addFile);

        getClassesToScan(classesDir).stream()
            .peek(p -> LOG.debug(" +class {}", p))
            .forEach(spotBugsProject::addFile);

        classpath.stream()
            .map(SpotBugsRunner::pathToString)
            .peek(p -> LOG.debug(" +aux {}", p))
            .forEach(spotBugsProject::addAuxClasspathEntry);

        if (spotBugsProject.getFileList().isEmpty()) {
            throw new IllegalStateException("no source files");
        }

        return spotBugsProject;
    }

    private UserPreferences buildUserPreferences() {
        final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
        userPreferences.setEffort(EFFORT_DEFAULT);
        return userPreferences;
    }

    private static List<String> getClassesToScan(final Path classesDir) throws IOException {
        return Files.walk(classesDir)
            .filter(filterByExtension("class"))
            .filter(f -> !f.getFileName().toString().equals("module-info.class"))
            .map(SpotBugsRunner::pathToString)
            .collect(Collectors.toList());
    }

    private static Predicate<Path> filterByExtension(final String extension) {
        Objects.requireNonNull(extension);
        return p -> extension.equals(SystemUtil.getFileExtension(p.getFileName().toString()));
    }

    private static String pathToString(final Path file) {
        return file.toAbsolutePath().normalize().toString();
    }

}
