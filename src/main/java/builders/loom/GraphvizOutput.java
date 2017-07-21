/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.TaskInfo;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class GraphvizOutput {

    private GraphvizOutput() {
    }

    public static void generateDot(final ModuleRunner moduleRunner) {
        try {
            final Path reportDir = Files.createDirectories(Paths.get("loombuild", "reports"));
            final Path dotFile = reportDir.resolve(Paths.get("loom-products.dot"));

            try (PrintWriter pw = new PrintWriter(dotFile.toFile(), "UTF-8")) {
                writeTasks(moduleRunner, pw);
            }

            System.out.println("Products overview written to " + dotFile);
            System.out.println("Use Graphviz to visualize: `dot -Tpng " + dotFile
                + " > loom-products.png`");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeTasks(final ModuleRunner moduleRunner,
                                   final PrintWriter pw) {

        pw.println("digraph dependencies {");
        pw.println("    rankdir=\"RL\";");
        pw.println("    graph [splines=spline, nodesep=1];");
        pw.println("    node [shape=box];");

        for (final TaskInfo task : moduleRunner.configuredTasks()) {
            writeLabel(pw, task);
        }

        for (final TaskInfo task : moduleRunner.configuredTasks()) {
//            if (!task.getUsedProducts().isEmpty()) {
//                writeEdge(pw, task);
//            }
        }

        pw.println("}");
    }

    private static void writeLabel(final PrintWriter pw, final TaskInfo task) {
        pw.print("    ");
        pw.print(task.getName());
        final String label;
        if (task.isGoal()) {
            label = task.getName();
        } else {
            label = task.getName() + "\\n[" + task.getPluginName() + " Plugin]";
        }
        pw.print(" [label=\"" + label + "\"");

        if (task.isGoal()) {
            pw.print(", color=gold2, shape=tripleoctagon");
        } else if (task.isIntermediateProduct()) {
            pw.print(", color=grey, fontcolor=grey");
        }

        pw.print("]");

        pw.println(";");
    }

    private static void writeEdge(final PrintWriter pw, final ConfiguredTask task) {
        pw.print("    ");
        pw.print(task.getProvidedProduct());
        pw.print(" -> ");
        pw.print(constructValue(task.getUsedProducts()));
        pw.println(";");
    }

    private static String constructValue(final Collection<String> dependentNodes) {
        if (dependentNodes == null || dependentNodes.isEmpty()) {
            throw new IllegalArgumentException("dependentNodes must be > 0");
        }

        if (dependentNodes.size() == 1) {
            return dependentNodes.iterator().next();
        }

        return "{" + dependentNodes.stream()
            .collect(Collectors.joining(", "))
            + "}";
    }

}
