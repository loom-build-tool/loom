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

package builders.loom.plugin.findbugs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import builders.loom.api.AbstractTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;
import edu.umd.cs.findbugs.Priorities;

public class FindbugsTask extends AbstractTask {

    private static final int DEFAULT_PRIORITY_THRESHOLD = Priorities.NORMAL_PRIORITY;

    private final CompileTarget compileTarget;
    private int priorityThreshold;
    private boolean loadFbContrib;
    private boolean loadFindBugsSec;

    public FindbugsTask(final FindbugsPluginSettings pluginSettings,
                        final CompileTarget compileTarget) {

        this.compileTarget = Objects.requireNonNull(compileTarget);

        readBuildConfig(Objects.requireNonNull(pluginSettings));
    }

    private void readBuildConfig(final FindbugsPluginSettings pluginSettings) {

        priorityThreshold = Optional.ofNullable(pluginSettings.getPriorityThreshold())
            .map(prio -> resolvePriority(prio.toUpperCase()))
            .orElse(DEFAULT_PRIORITY_THRESHOLD);

        final List<String> customPlugins =
            parsePropValue(pluginSettings.getCustomPlugins());

        if (customPlugins.remove("FbContrib")) {
            loadFbContrib = true;
        }
        if (customPlugins.remove("FindSecBugs")) {
            loadFindBugsSec = true;
        }

        Preconditions.checkState(
            customPlugins.isEmpty(),
            "Unknown findbugs custom plugin(s): " + customPlugins);

    }

    private int resolvePriority(final String prio) {
        return Stream.of(Priorities.class.getDeclaredFields())
            .filter(f -> f.getType().equals(int.class))
            .filter(f -> f.getName().equals(prio + "_PRIORITY"))
            .findFirst()
            .map(f -> {
                try {
                    return f.getInt(null);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            })
            .orElseThrow(() -> new IllegalStateException("Unknown FindBugs priority: " + prio));
    }

    private List<String> parsePropValue(final String input) {
        if (input == null) {
            return Collections.emptyList();
        }

        return
            Arrays.stream(input.split(","))
            .map(String::trim)
            .filter(str -> !str.isEmpty())
            .collect(Collectors.toList());
    }

    @Override
    public TaskResult run() throws Exception {
        if (!getSourceTree().isPresent() || !getClasses().isPresent()) {
            return completeSkip();
        }

        FindbugsSingleton.initFindbugs(loadFbContrib, loadFindBugsSec);

        final Path reportPath = LoomPaths.reportDir(getModule().getModuleName(), "findbugs")
            .resolve(compileTarget.name().toLowerCase());

        new FindbugsRunner(reportPath, getSourceTree().get().getSourceFiles(),
            getClasses().get().getClassesDir(), calcClasspath(), priorityThreshold)
            .executeFindbugs();

        return completeOk(product(reportPath));
    }

    private ReportProduct product(final Path reportPath) {
        switch (compileTarget) {
            case MAIN:
                return new ReportProduct(reportPath, "Findbugs main report");
            case TEST:
                return new ReportProduct(reportPath, "Findbugs test report");
            default:
                throw new IllegalStateException();
        }
    }

    private Optional<SourceTreeProduct> getSourceTree() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("source", SourceTreeProduct.class);
            case TEST:
                return useProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private Optional<CompilationProduct> getClasses() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("compilation", CompilationProduct.class);
            case TEST:
                return useProduct("testCompilation", CompilationProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    // FIXME multi-module
    private List<Path> calcClasspath() throws InterruptedException {
        final List<Path> classpath = new ArrayList<>();

        switch (compileTarget) {
            case MAIN:
                useProduct("compileDependencies",
                    ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            case TEST:
                final Path buildPath = LoomPaths.BUILD_DIR.resolve(
                    Paths.get("compilation", "main", getModule().getModuleName()));
                classpath.add(buildPath);

                useProduct("testDependencies",
                    ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            default:
                throw new IllegalArgumentException("Unknown target: " + compileTarget);
        }
        return classpath;
    }

}
