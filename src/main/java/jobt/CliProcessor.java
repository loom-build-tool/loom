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

    private final ProcessMonitor processMonitor;
    private final String[] args;
    private final Map<String, Class<? extends Task>> taskClasses;

    public CliProcessor(final ProcessMonitor processMonitor, final String[] args) {
        this.processMonitor = processMonitor;
        this.args = args;
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

    public void run() throws Exception {

        processMonitor.updateProcess("Read configuration");
        final BuildConfig buildConfig = readConfig();

        processMonitor.newProcess("\uD83D\uDD0D Read configuration",
            String.format("Start building %s version %s",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion()));


        Thread.sleep(10000);


        final Options options = new Options();

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);

        final PluginRegistry pluginRegistry = new PluginRegistry(buildConfig);

        for (final String arg : resolveTasks(cmd.getArgs())) {
            doArg(pluginRegistry, arg);
        }
    }

    private List<String> resolveTasks(final String[] args) {
        if (args.length == 0) {
            return Arrays.asList("clean", "compileJava", "assemble");
            //return Arrays.asList("compileJava", "processResources", "compileTestJava", "processTestResources", "test", "jar");
        }

        final List<String> tasks = new ArrayList<>();
        for (final String arg : args) {
            switch (arg) {
                case "build":
                    tasks.addAll(Arrays.asList("compileJava"));
                    //tasks.addAll(Arrays.asList("compileJava", "processResources", "compileTestJava", "processTestResources", "test", "jar"));
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
