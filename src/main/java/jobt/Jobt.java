package jobt;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import jobt.config.BuildConfig;
import jobt.plugin.JavaPlugin;
import jobt.plugin.Plugin;

public class Jobt {

    public static void main(final String[] args) {
        final long startTime = System.nanoTime();
        System.out.println("Java Optimized Build Tool v1.0.0");

        try {
            System.out.println("\uD83D\uDD0D Read configuration");
            final BuildConfig buildConfig = readConfig();

            System.out.printf("Start building %s version %s%n",
                buildConfig.getProject().getArchivesBaseName(),
                buildConfig.getProject().getVersion());

            final List<String> compileDependencies = buildConfig.getDependencies();
            final String classPath = new MavenResolver()
                .buildClasspath(compileDependencies, "compile");

            final Build build = initBuild(buildConfig);

            build.compile(classPath);

            final List<String> testDependencies = new ArrayList<>(compileDependencies);
            if (buildConfig.getTestDependencies() != null) {
                testDependencies.addAll(buildConfig.getTestDependencies());
            }

            final String testClassPath = new MavenResolver()
                .buildClasspath(testDependencies, "test");

            build.test(testClassPath);

            build.assemble();
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Build failed - " + e.getLocalizedMessage());
            System.exit(1);
        }

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.printf("✨ Built in %.2fs%n", duration);
    }

    private static BuildConfig readConfig() throws IOException {
        final Yaml yaml = new Yaml();

        final Path buildFile = Paths.get("build.yml");
        if (!Files.isRegularFile(buildFile)) {
            throw new IOException("No build.yml found");
        }

        try (final Reader resourceAsStream = Files.newBufferedReader(buildFile,
            StandardCharsets.UTF_8)) {
            return yaml.loadAs(resourceAsStream, BuildConfig.class);
        }
    }

    private static Build initBuild(final BuildConfig buildConfig)
        throws InstantiationException, IllegalAccessException, InvocationTargetException,
        NoSuchMethodException {

        final Build build = new Build();

        final Map<String, Class<? extends Plugin>> stringClassMap = buildPluginRegistry();

        for (final String pluginName : buildConfig.getPlugins()) {
            final Class<? extends Plugin> pluginClass = stringClassMap.get(pluginName);
            if (pluginClass == null) {
                throw new IllegalStateException("Unknown plugin: " + pluginName);
            }

            build.registerPlugin(
                pluginClass.getConstructor(BuildConfig.class).newInstance(buildConfig));
        }

        return build;
    }

    private static Map<String, Class<? extends Plugin>> buildPluginRegistry() {
        final Map<String, Class<? extends Plugin>> pluginMap = new HashMap<>();
        pluginMap.put("java", JavaPlugin.class);
        return pluginMap;
    }

}
