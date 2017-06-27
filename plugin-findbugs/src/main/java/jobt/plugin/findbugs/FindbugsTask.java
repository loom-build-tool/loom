package jobt.plugin.findbugs;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.ClasspathProduct;
import jobt.api.CompileTarget;
import jobt.api.TaskStatus;
import jobt.util.Preconditions;

public class FindbugsTask extends AbstractTask {

    public static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    public static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");
    public static final Path BUILD_MAIN_PATH = Paths.get("jobtbuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("jobtbuild", "classes", "test");
    public static final Path REPORT_PATH = Paths.get("jobtbuild", "reports", "findbugs");

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsTask.class);

    private static final Map<String, Integer> PRIORITIES_MAP = buildPrioritiesMap();

    private final Path sourceDir;
    private final Path classesDir;
    private final CompileTarget compileTarget;

    private Optional<Integer> priorityThreshold;

    public FindbugsTask(
        final BuildConfig buildConfig, final CompileTarget compileTarget) {

        readBuildConfig(Objects.requireNonNull(buildConfig));
        this.compileTarget = Objects.requireNonNull(compileTarget);

        switch (compileTarget) {
            case MAIN:
                this.sourceDir = SRC_MAIN_PATH;
                this.classesDir = BUILD_MAIN_PATH;
                break;
            case TEST:
                this.sourceDir = SRC_TEST_PATH;
                this.classesDir = BUILD_TEST_PATH;
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

    }

    private void readBuildConfig(final BuildConfig buildConfig) {

        priorityThreshold = Optional.ofNullable(
            buildConfig.getConfiguration().get("findbugsPriorityThreshold"))
            .map(PRIORITIES_MAP::get)
            .map(prio -> Objects.requireNonNull(prio, "Invalid priority thresold " + prio));

    }

    @Override
    public void prepare() throws Exception {
        FindbugsRunner.startupFindbugsAsync();
    }

    @Override
    public TaskStatus run() throws Exception {
        checkDirs();

        final ClasspathProduct compileOutput = new ClasspathProduct(Collections.singletonList(classesDir));

        final List<BugInstance> bugs = new FindbugsRunner(
            sourceDir,
            compileOutput.getSingleEntry(),
            calcClasspath(),
            priorityThreshold
            )
            .executeFindbugs();


        if (bugs.isEmpty()) {
            return TaskStatus.OK;
        }

        final StringBuilder report = new StringBuilder();
        for (final BugInstance bug : bugs) {
            report.append(String.format(" >>> %s ", bug.getMessage()));
            report.append('\n');
        }
        LOG.warn("Findbugs report for {}: \n{}", compileTarget, report.toString());

        throw new IllegalStateException(
            String.format("Findbugs reported %d bugs!", bugs.size()));
    }

    private ClasspathProduct calcClasspath() {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("compileDependencies", ClasspathProduct.class);
            case TEST:
                final List<Path> classpath = new ArrayList<>();
                classpath.add(BUILD_MAIN_PATH);
                classpath.addAll(
                    getUsedProducts().readProduct("testDependencies", ClasspathProduct.class).getEntries());
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

    private void checkDirs() {
        Preconditions.checkState(Files.isDirectory(sourceDir),
            "Source dir <%s> does not exist", sourceDir);
        Preconditions.checkState(Files.isDirectory(classesDir),
            "Classes dir <%s> does not exist", classesDir);
    }

}
