package builders.loom;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class Main {

    private static final Path EXAMPLES_PATH = Paths.get("/Users/sostermayr/workspace_java9/loom-example/java9");

    public static void main(String[] args) throws Exception {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            null, null, StandardCharsets.UTF_8)) {

            //            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH,
            //                new ArrayList<>(classpath));


            // TODO set via     setLocationForModule ?
            final Path o = Paths.get("/tmp/mainout");
            Files.createDirectories(o);

            fileManager.setLocation(StandardLocation.MODULE_PATH,
                List.of(EXAMPLES_PATH.resolve("loombuild/compilation/main").toFile()));

            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(o));

            //            LOG.warn("srcpath {}", Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));

//                    fileManager.setLocationForModule(StandardLocation.MODULE_SOURCE_PATH,
//                        "builders.loom.example.app",
//                        Collections.singletonList(Paths.get("/Users/oliver/workspace/loom-example/java9/modules/builders.loom.example.app/src/main/java")));
//
//                    fileManager.setLocationForModule(StandardLocation.MODULE_SOURCE_PATH,
//                        "builders.loom.example.api",
//                        Collections.singletonList(Paths.get("/Users/oliver/workspace/loom-example/java9/modules/builders.loom.example.api/src/main/java")));

//                    fileManager.setLocationForModule(StandardLocation.MODULE_PATH,
//                        "builders.loom.example.api",
//                        Collections.singletonList(Paths.get("/Users/oliver/workspace/loom-example/java9/loombuild/compilation/main/builders.loom.example.api")));

            final List<File> files = List.of(EXAMPLES_PATH.resolve("modules/builders.loom.example.app/src/main/java/builders/loom/example/app/Main.java").toFile());

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromFiles(files);

            final List<String> options = buildOptions();

            final JavaCompiler.CompilationTask compilerTask = compiler
                .getTask(null, fileManager, null, options, null, compUnits);

            if (!compilerTask.call()) {
                throw new IllegalStateException("Java compile failed");
            }
        }
    }


    private static List<String> buildOptions() {
        final List<String> options = new ArrayList<>();
//        options.add("-d");
//        options.add("/tmp/mainout");

        options.add("-encoding");
        options.add("UTF-8");

        options.add("-Xlint:all");

        // http://blog.ltgt.net/most-build-tools-misuse-javac/

//        options.add("-sourcepath");
//        options.add("");

//        options.add("--module-source-path");
//        options.add("src/main/java");

        options.add("-Xpkginfo:always");


        return options;
    }


}
