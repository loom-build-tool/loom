package jobt.plugin.mavenresolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.DependencyScope;


public class MavenResolveMain {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolveMain.class);

    public static void main(final String[] args) throws IOException {


        Files.deleteIfExists(Paths.get(".jobt/compile-dependencies"));
        Files.deleteIfExists(Paths.get("/Users/sostermayr/.jobt/repository/org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar"));
        Files.deleteIfExists(Paths.get("/Users/sostermayr/.jobt/repository/org/apache/httpcomponents/httpcore/4.4.6/httpcore-4.4.6.jar"));

        final MavenResolver resolver = new MavenResolver();

        final List<String> deps = new ArrayList<>();
        deps.add("org.apache.httpcomponents:httpclient:4.5.3");
        final List<Path> list = resolver.resolve(deps, DependencyScope.COMPILE);

        list.forEach(p -> System.out.println(" - " + p));



//        - /Users/sostermayr/.jobt/repository/org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar
//        - /Users/sostermayr/.jobt/repository/org/apache/httpcomponents/httpcore/4.4.6/httpcore-4.4.6.jar
//        - /Users/sostermayr/.jobt/repository/commons-logging/commons-logging/1.2/commons-logging-1.2.jar
//        - /Users/sostermayr/.jobt/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar

    }
}
