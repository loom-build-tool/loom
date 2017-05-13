package jobt;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.yaml.snakeyaml.Yaml;

import jobt.config.BuildConfig;
import jobt.plugin.PluginRegistry;
import jobt.task.AssembleTask;
import jobt.task.CleanTask;
import jobt.task.CompileTask;
import jobt.task.Task;

public class CliProcessor {

    private final long startTime;
    private final Map<String, Class<? extends Task>> taskClasses;

    public CliProcessor() {
        startTime = System.nanoTime();
        taskClasses = buildTaskRegistry();
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

    private static Map<String, Class<? extends Task>> buildTaskRegistry() {
        final Map<String, Class<? extends Task>> taskMap = new HashMap<>();
        taskMap.put("clean", CleanTask.class);
        taskMap.put("compileJava", CompileTask.class);
        taskMap.put("assemble", AssembleTask.class);
        return taskMap;
    }

    public void run(final String[] args) throws Exception {
        Progress.newStatus("Read configuration");
        final BuildConfig buildConfig = readConfig();
        Progress.complete();

        Progress.log(String.format("Initialized configuration for %s version %s",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion()));

        final Options options = new Options();

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);

        final PluginRegistry pluginRegistry = new PluginRegistry(buildConfig);

        for (final String arg : resolveTasks(cmd.getArgs())) {
            doArg(pluginRegistry, arg);
        }

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        Progress.log(String.format("✨ Built in %.2fs%n", duration));

    }

    private List<String> resolveTasks(final String[] targets) {
        if (targets.length == 0) {
            return Arrays.asList("compileJava", "assemble");
        }

        final List<String> tasks = new ArrayList<>();
        for (final String arg : tasks) {
            switch (arg) {
                case "build":
                    tasks.addAll(Arrays.asList("compileJava", "assemble"));
                    break;
                default:
                    tasks.add(arg);
            }
        }

        return tasks;
    }

    private void doArg(final PluginRegistry pluginRegistry, final String arg) throws Exception {
        final Class<? extends Task> taskClass = taskClasses.get(arg);
        if (taskClass == null) {
            throw new IllegalArgumentException("Task " + arg + " is unknown");
        }

        taskClass.getConstructor(PluginRegistry.class).newInstance(pluginRegistry).run();
    }

}
