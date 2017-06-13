package jobt.plugin.mavenresolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class MavenResolverTask implements Task {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolverTask.class);

    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

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

        System.out.println("CONS MavenResolverTask ("+dependencyScope+")");
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {

        System.out.println("Run MavenResolverTask...");

        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        final CompletableFuture<List<Path>> promise;

        switch (dependencyScope) {
            case COMPILE:
                pool.submit(compileScope());
                return TaskStatus.OK;
            case TEST:
                pool.submit(testScope());
                return TaskStatus.OK;
        }


        // TODO support test
        //        buildConfig.getTestDependencies()


        //        final Future<List<Path>> resolving = mavenResolver.resolveDependencies(dependencies, "compile");

        //        executionContext.setResolvingCompileDependencies(resolving);

        throw new IllegalStateException();
    }


    private Callable<?> compileScope() throws DependencyCollectionException, DependencyResolutionException, IOException {


        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        return () ->
        executionContext.getCompileDependenciesPromise().complete(mavenResolver.resolve(dependencies, DependencyScope.COMPILE));

    }

    private Callable<?> testScope() throws DependencyCollectionException, DependencyResolutionException, IOException {


        final List<String> dependencies = new ArrayList<>();

        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        if (buildConfig.getTestDependencies() != null) {
            dependencies.addAll(buildConfig.getTestDependencies());
        }

        return () ->
        executionContext.getTestDependenciesPromise().complete(mavenResolver.resolve(dependencies, DependencyScope.TEST));

    }

}
