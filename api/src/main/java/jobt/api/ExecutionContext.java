package jobt.api;

import java.net.URL;
import java.util.List;

public interface ExecutionContext {

    List<URL> getCompileClasspath() throws InterruptedException;

    void setCompileClasspath(List<URL> compileClasspath);

    List<URL> getTestClasspath() throws InterruptedException;

    void setTestClasspath(List<URL> testClasspath);

}
