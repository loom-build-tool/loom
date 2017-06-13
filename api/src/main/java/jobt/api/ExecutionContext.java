package jobt.api;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ExecutionContext {

    CompletableFuture<List<Path>> getCompileDependenciesPromise();

    List<Path> getResolvedCompileDependencies() throws InterruptedException;

    CompletableFuture<List<Path>> getTestDependenciesPromise();

    List<Path> getResolvedTestDependencies() throws InterruptedException;

    List<URL> getCompileClasspath() throws InterruptedException;

    void setCompileClasspath(List<URL> compileClasspath);

    List<URL> getTestClasspath() throws InterruptedException;

    void setTestClasspath(List<URL> testClasspath);


}
