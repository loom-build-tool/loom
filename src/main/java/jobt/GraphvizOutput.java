package jobt;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jobt.api.TaskGraphNode;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class GraphvizOutput {

    private GraphvizOutput() {
    }

    public static void generateDot(final Map<String, TaskGraphNodeImpl> tasks) {
        try {
            final Path reportDir = Files.createDirectories(Paths.get("jobtbuild", "reports"));
            final Path tasksFile = reportDir.resolve(Paths.get("tasks.dot"));

            try (PrintWriter pw = new PrintWriter(tasksFile.toFile(), "UTF-8")) {
                writeTasks(tasks, pw);
            }

            System.out.println("Task overview written to " + tasksFile);
            System.out.println("Use Graphviz to visualize: `dot -Tpng " + tasksFile
                + " > tasks.png`");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeTasks(final Map<String, TaskGraphNodeImpl> tasks,
                                   final PrintWriter pw) {
        pw.println("digraph dependencies {");
        pw.println("    rankdir=\"RL\";");
        pw.println("    graph [splines=ortho, nodesep=1];");
        pw.println("    node [shape=rectangle];");

        final Set<String> standaloneTasks = new HashSet<>(tasks.keySet());
        for (final Map.Entry<String, TaskGraphNodeImpl> entry : tasks.entrySet()) {
            final List<TaskGraphNode> dependencies = entry.getValue().getDependentNodes();
            if (!dependencies.isEmpty()) {
                standaloneTasks.remove(entry.getKey());
                for (final TaskGraphNode task : dependencies) {
                    standaloneTasks.remove(task.getName());
                }
            }
        }

        for (final String standaloneTask : standaloneTasks) {
            pw.print("    ");
            pw.print(standaloneTask);
            pw.println(";");
        }

        for (final Map.Entry<String, TaskGraphNodeImpl> entry : tasks.entrySet()) {
            final List<TaskGraphNode> dependentNodes = entry.getValue().getDependentNodes();
            if (!dependentNodes.isEmpty()) {
                pw.print("    ");
                pw.print(entry.getKey());
                pw.print(" -> ");
                pw.println(constructValue(dependentNodes));
            }
        }
        pw.println("}");
    }

    private static String constructValue(final List<TaskGraphNode> dependentNodes) {
        final StringBuilder sb = new StringBuilder();

        if (dependentNodes.size() > 1) {
            sb.append("{");
        }

        sb.append(dependentNodes.stream()
            .map(TaskGraphNode::getName)
            .collect(Collectors.joining(", ")));

        if (dependentNodes.size() > 1) {
            sb.append("}");
        }

        return sb.toString();
    }

}
