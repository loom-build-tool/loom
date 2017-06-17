package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;

import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class MavenResolverTask implements Task {

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private final MavenResolver mavenResolver;

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
                compileScope();
                break;
            case TEST:
                testScope();
                break;
            default:
                throw new IllegalStateException();
        }

        return TaskStatus.OK;
    }

    private void compileScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());

        executionContext.getCompileDependenciesPromise()
            .complete(mavenResolver.resolve(dependencies, DependencyScope.COMPILE));
    }

    private void testScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        executionContext.getTestDependenciesPromise()
            .complete(mavenResolver.resolve(dependencies, DependencyScope.TEST));
    }

}
