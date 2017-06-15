package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class MavenResolverTask implements Task {

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private final MavenResolver mavenResolver = new MavenResolver();

    public MavenResolverTask(
        final DependencyScope dependencyScope,
        final BuildConfig buildConfig,
        final ExecutionContext executionContext) {

        this.dependencyScope = dependencyScope;
        this.buildConfig = buildConfig;
        this.executionContext = executionContext;

    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {

        switch (dependencyScope) {
            case COMPILE:
                MavenExecutor.submit(compileScope());
                return TaskStatus.OK;
            case TEST:
                MavenExecutor.submit(testScope());
                return TaskStatus.OK;
            default:
                throw new IllegalStateException();
        }
    }

    private Callable<?> compileScope() throws Exception {

        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        return () ->
            executionContext.getCompileDependenciesPromise()
            .complete(mavenResolver.resolve(dependencies, DependencyScope.COMPILE));

    }

    private Callable<?> testScope() throws Exception {

        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        if (buildConfig.getTestDependencies() != null) {
            dependencies.addAll(buildConfig.getTestDependencies());
        }

        return () ->
            executionContext.getTestDependenciesPromise()
            .complete(mavenResolver.resolve(dependencies, DependencyScope.TEST));

    }

}
