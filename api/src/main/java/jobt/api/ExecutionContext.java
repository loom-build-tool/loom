package jobt.api;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface ExecutionContext {

    void setCompileClasspath(List<URL> compileClasspath);
    List<URL> getCompileClasspath() throws InterruptedException;

    void setTestClasspath(List<URL> testClasspath);
    List<URL> getTestClasspath() throws InterruptedException;

//    void setCompileDependencies(List<Path> compileDependencies);
//    List<Path> getCompileDependencies();

//    void setTestDependencies(List<Path> testDependencies);
//    List<Path> getTestDependencies();


    Map<String, ProductPromise> getProducts();

}
