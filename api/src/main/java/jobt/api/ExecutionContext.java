package jobt.api;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface ExecutionContext {

    void setCompileClasspath(List<URL> compileClasspath);
    List<URL> getCompileClasspath() throws InterruptedException;

    void setTestClasspath(List<URL> testClasspath);
    List<URL> getTestClasspath() throws InterruptedException;

    void setCompileDependencies(List<Path> compileDependencies);
    List<Path> getCompileDependencies();

    void setTestDependencies(List<Path> testDependencies);
    List<Path> getTestDependencies();

}
