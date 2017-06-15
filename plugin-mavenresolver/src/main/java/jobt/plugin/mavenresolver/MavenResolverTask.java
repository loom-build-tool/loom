package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class MavenResolverTask implements Task {

    private static final int THREAD_COUNT = 2;

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private final MavenResolver mavenResolver;

    private final ExecutorService pool = Executors.newFixedThreadPool(
        THREAD_COUNT, r -> {
            final Thread thread = new Thread(r);
            thread.setName("thread-" + MavenResolverTask.class.getSimpleName());
            thread.setDaemon(true);
            return thread;
        });

    public MavenResolverTask(final DependencyScope dependencyScope,
                             final BuildConfig buildConfig,
                             final ExecutionContext executionContext,
                             final MavenResolver mavenResolver) {

        this.dependencyScope = dependencyScope;
        this.buildConfig = buildConfig;
        this.executionContext = executionContext;
        this.mavenResolver = mavenResolver;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        switch (dependencyScope) {
            case COMPILE:
                pool.submit(compileScope());
                break;
            case TEST:
                pool.submit(testScope());
                break;
            default:
                throw new IllegalStateException();
        }

        return TaskStatus.OK;
    }

    private Callable<?> compileScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        return () ->
            executionContext.getCompileDependenciesPromise()
                .complete(mavenResolver.resolve(dependencies, DependencyScope.COMPILE));
    }

    private Callable<?> testScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        return () ->
            executionContext.getTestDependenciesPromise()
                .complete(mavenResolver.resolve(dependencies, DependencyScope.TEST));
    }

}
