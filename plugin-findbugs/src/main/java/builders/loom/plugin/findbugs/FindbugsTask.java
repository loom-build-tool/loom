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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import builders.loom.api.AbstractTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;
import edu.umd.cs.findbugs.Priorities;

public class FindbugsTask extends AbstractTask {

    public static final Path BUILD_MAIN_PATH = Paths.get("loombuild", "classes", "main");
    private static final Map<String, Integer> PRIORITIES_MAP = buildPrioritiesMap();

    private final CompileTarget compileTarget;
    private final Path reportPath;
    private Optional<Integer> priorityThreshold;
    private boolean loadFbContrib;
    private boolean loadFindBugsSec;

    public FindbugsTask(final FindbugsPluginSettings pluginSettings,
                        final CompileTarget compileTarget) {

        this.compileTarget = Objects.requireNonNull(compileTarget);

        reportPath = LoomPaths.REPORT_PATH.resolve(Paths.get("findbugs",
            compileTarget.name().toLowerCase()));

        readBuildConfig(Objects.requireNonNull(pluginSettings));
    }

    private void readBuildConfig(final FindbugsPluginSettings pluginSettings) {

        priorityThreshold =
            pluginSettings.getPriorityThreshold()
            .map(PRIORITIES_MAP::get)
            .map(prio -> Objects.requireNonNull(prio, "Invalid priority threshold " + prio));

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
    public TaskStatus run() throws Exception {

        if (Files.notExists(getSourceTree().getSrcDir())
            || Files.notExists(getClasses().getClassesDir())) {
            return complete(TaskStatus.SKIP);
        }

        FindbugsSingleton.initFindbugs(loadFbContrib, loadFindBugsSec);

        new FindbugsRunner(reportPath, getSourceTree().getSrcDir(),
            getClasses().getClassesDir(), calcClasspath(), priorityThreshold).executeFindbugs();

        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProduct().complete("findbugsMainReport",
                    new ReportProduct(reportPath, "Findbugs main report"));
                return status;
            case TEST:
                getProvidedProduct().complete("findbugsTestReport",
                    new ReportProduct(reportPath, "Findbugs test report"));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private SourceTreeProduct getSourceTree() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("source", SourceTreeProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private CompilationProduct getClasses() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("compilation", CompilationProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testCompilation", CompilationProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private ClasspathProduct calcClasspath() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("compileDependencies",
                    ClasspathProduct.class);
            case TEST:
                final List<Path> classpath = new ArrayList<>();
                classpath.add(BUILD_MAIN_PATH);
                classpath.addAll(
                    getUsedProducts().readProduct("testDependencies",
                        ClasspathProduct.class).getEntries());
                return new ClasspathProduct(classpath);
            default:
                throw new IllegalArgumentException("Unknown target: " + compileTarget);
        }
    }

    private static Map<String, Integer> buildPrioritiesMap() {
        final Field[] fields = Priorities.class.getDeclaredFields();

        return
            Stream.of(fields)
            .filter(f -> f.getType().equals(int.class))
            .collect(Collectors.toMap(
                f -> f.getName().replaceAll("_PRIORITY", ""),
                f -> {
                    try {
                        return f.getInt(null);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }));
    }

}
