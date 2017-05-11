package jobt.plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.MavenResolver;
import jobt.config.BuildConfig;

public class JavaPlugin extends AbstractPlugin {

    private final BuildConfig buildConfig;

    public JavaPlugin(final BuildConfig buildConfig) {
        super("compile");
        this.buildConfig = buildConfig;
    }

    @Override
    public void run() throws Exception {
        final Path jobtbuild = Paths.get("jobtbuild");
        if (Files.notExists(jobtbuild)) {
            Files.createDirectory(jobtbuild);
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diagnosticListener =
            diagnostic -> System.out.println("DIAG: " + diagnostic);

        final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(diagnosticListener, null, StandardCharsets.UTF_8);

        final List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(jobtbuild.toString());

        final Map<String, String> config = buildConfig.getConfiguration();
        final String sourceCompatibility = config.get("sourceCompatibility");
        if (sourceCompatibility != null) {
            options.add("-source");
            options.add(sourceCompatibility);
        }

        final String targetCompatibility = config.get("targetCompatibility");
        if (targetCompatibility != null) {
            options.add("-target");
            options.add(targetCompatibility);
        }

        options.add("-cp");
        options.add(buildClasspath(buildConfig.getDependencies()));

        //System.out.println(options);

        final File[] srcFiles = Files.walk(Paths.get("src/main/java"))
                .filter(f -> Files.isRegularFile(f))
                .filter(f -> f.toString().endsWith(".java"))
                .map(Path::toFile)
                .toArray(File[]::new);

        final Iterable<? extends JavaFileObject> compUnits =
            fileManager.getJavaFileObjects(srcFiles);

        if (!compiler.getTask(null, fileManager, diagnosticListener, options, null, compUnits).call()) {
            throw new IllegalStateException("Compile failed");
        }
    }

    private String buildClasspath(final List<String> dependencies) throws DependencyCollectionException, DependencyResolutionException {
        return new MavenResolver()
            .buildClasspath(dependencies, "compile");
    }

}
