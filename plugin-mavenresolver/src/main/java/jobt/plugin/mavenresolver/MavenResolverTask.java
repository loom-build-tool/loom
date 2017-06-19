package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jobt.api.BuildConfig;
import jobt.api.Classpath;
import jobt.api.DependencyScope;
import jobt.api.ProvidedProducts;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.api.UsedProducts;

public class MavenResolverTask implements Task {

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(
        r -> {
            final Thread thread = new Thread(r);
            thread.setName("thread-mavenresolver");
            thread.setDaemon(true);
            return thread;
        });

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private final MavenResolver mavenResolver;

    private final UsedProducts input;
    private final ProvidedProducts output;

    public MavenResolverTask(final DependencyScope dependencyScope,
                             final BuildConfig buildConfig, final MavenResolver mavenResolver,
                             final UsedProducts input, final ProvidedProducts output) {

        this.dependencyScope = dependencyScope;
        this.buildConfig = buildConfig;
        this.input = input;
        this.output = output;
        this.mavenResolver = mavenResolver;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {

        switch (dependencyScope) {
            case COMPILE:
                WORKER.execute(compileScope());
                return TaskStatus.OK;
            case TEST:
                WORKER.execute(testScope());
                return TaskStatus.OK;
            default:
                throw new IllegalStateException();
        }
    }

    private Runnable compileScope() throws Exception {

        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        return () ->
            output.complete("compileDependencies",
                new Classpath(mavenResolver.resolve(dependencies, DependencyScope.COMPILE)));

    }

    private Runnable testScope() throws Exception {

        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        if (buildConfig.getTestDependencies() != null) {
            dependencies.addAll(buildConfig.getTestDependencies());
        }

        return () ->
            output.complete("testDependencies",
                new Classpath(mavenResolver.resolve(dependencies, DependencyScope.TEST)));

    }

}
