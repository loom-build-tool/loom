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
        mavenResolver.init();
    }

    @Override
    public TaskStatus run() throws Exception {
        switch (dependencyScope) {
            case COMPILE:
                compileScope();
                return TaskStatus.OK;
            case TEST:
                testScope();
                return TaskStatus.OK;
            default:
                throw new IllegalStateException("Unknown scope: " + dependencyScope);
        }
    }

    private void compileScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        executionContext.setCompileDependencies(mavenResolver.resolve(dependencies,
            DependencyScope.COMPILE));
    }

    private void testScope() throws Exception {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        executionContext.setTestDependencies(mavenResolver.resolve(dependencies,
            DependencyScope.TEST));
    }

}
