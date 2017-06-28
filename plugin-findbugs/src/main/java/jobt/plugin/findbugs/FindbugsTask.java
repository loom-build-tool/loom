package jobt.plugin.findbugs;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import jobt.api.CompilationProduct;
import jobt.api.CompileTarget;
import jobt.api.ReportProduct;
import jobt.api.SourceTreeProduct;
import jobt.api.TaskStatus;

public class FindbugsTask extends AbstractTask {

    public static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    public static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");
    public static final Path BUILD_MAIN_PATH = Paths.get("jobtbuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("jobtbuild", "classes", "test");
    public static final Path REPORT_PATH = Paths.get("jobtbuild", "reports", "findbugs");

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsTask.class);

    private static final Map<String, Integer> PRIORITIES_MAP = buildPrioritiesMap();

    private final CompileTarget compileTarget;

    private Optional<Integer> priorityThreshold;

    public FindbugsTask(
        final BuildConfig buildConfig, final CompileTarget compileTarget) {

        readBuildConfig(Objects.requireNonNull(buildConfig));
        this.compileTarget = Objects.requireNonNull(compileTarget);

    }

    private void readBuildConfig(final BuildConfig buildConfig) {

        priorityThreshold = Optional.ofNullable(
            buildConfig.getConfiguration().get("findbugsPriorityThreshold"))
            .map(PRIORITIES_MAP::get)
            .map(prio -> Objects.requireNonNull(prio, "Invalid priority thresold " + prio));

    }

    @Override
    public TaskStatus run() throws Exception {
        FindbugsRunner.startupFindbugsAsync();

        if (Files.notExists(getSourceTree().getSrcDir())
            || Files.notExists(getClasses().getClassesDir())) {
            return TaskStatus.SKIP;
        }

        final List<BugInstance> bugs = new FindbugsRunner(
            getSourceTree().getSrcDir(),
            getClasses().getClassesDir(),
            calcClasspath(),
            priorityThreshold
            )
            .executeFindbugs();


        if (bugs.isEmpty()) {
            return complete(TaskStatus.OK);
        }

        final StringBuilder report = new StringBuilder();
        for (final BugInstance bug : bugs) {
            report.append(String.format(" >>> %s ", bug.getMessage()));
            report.append('\n');
        }
        LOG.warn("Findbugs report for {}: \n{}", compileTarget, report);

        throw new IllegalStateException(
            String.format("Findbugs reported %d bugs!", bugs.size()));
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("findbugsMainReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("findbugsTestReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private SourceTreeProduct getSourceTree() {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("source", SourceTreeProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private CompilationProduct getClasses() {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("compilation", CompilationProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testCompilation", CompilationProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private ClasspathProduct calcClasspath() {
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
