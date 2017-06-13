package jobt.plugin.findbugs;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import jobt.api.CompileTarget;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.util.Preconditions;


public class FindbugsTask implements Task {

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsTask.class);

    public static final Path SRC_MAIN_PATH = Paths.get("src/main/java");
    public static final Path SRC_TEST_PATH = Paths.get("src/test/java");
    public static final Path BUILD_MAIN_PATH = Paths.get("jobtbuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("jobtbuild", "classes", "test");
    public static final Path REPORT_PATH = Paths.get("jobtbuild", "reports", "findbugs");

    private final ExecutionContext executionContext;
    private final Path sourceDir;
    private final Path classesDir;
    private final CompileTarget compileTarget;


    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });


    public FindbugsTask(final CompileTarget compileTarget,
        final ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.compileTarget = compileTarget;

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

    @Override
    public void prepare() throws Exception {

        pool.submit(() -> {
            FindBugsRunner.initFindBugs();

        });

    }


    @Override
    public TaskStatus run() throws Exception {
        checkDirs();

        final Classpath compileOutput = new Classpath(Collections.singletonList(classesDir));

        final List<BugInstance> bugs = new FindBugsRunner(
            sourceDir,
            compileOutput.getSingleEntry(),
            calcClasspath())
            .findMyBugs();

        for (final BugInstance bug : bugs) {

            System.out.println("bug #"+bug.getMessage());

        }

        return TaskStatus.OK;
    }

    private List<URL> calcClasspath() {
        try {
            final List<URL> classpathElements = new ArrayList<>();
            switch(compileTarget) {
                case MAIN:
                    classpathElements.addAll(executionContext.getCompileClasspath());
                    break;
                case TEST:
                    classpathElements.addAll(executionContext.getCompileClasspath());
                    classpathElements.addAll(executionContext.getTestClasspath());
                    break;
            }
            return classpathElements;
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void checkDirs() {
        Preconditions.checkState(Files.isDirectory(sourceDir),
            "Source dir <%s> does not exist", sourceDir);
        Preconditions.checkState(Files.isDirectory(classesDir),
            "Classes dir <%s> does not exist", classesDir);
    }

}
