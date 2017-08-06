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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.plugin.GoalInfo;
import builders.loom.plugin.TaskInfo;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class GraphvizOutput {

    private GraphvizOutput() {
    }

    public static void generateDot(final ModuleRunner moduleRunner) {
        try {
            final Path reportDir = Files.createDirectories(Paths.get("build", "reports"));
            final Path dotFile = reportDir.resolve(Paths.get("loom-products.dot"));

            try (PrintWriter pw = new PrintWriter(dotFile.toFile(), "UTF-8")) {
                writeTasks(moduleRunner, pw);
            }

            AnsiConsole.out().println(Ansi.ansi().a("Products overview written to ")
                .bold().a(dotFile).boldOff());
            AnsiConsole.out().println(Ansi.ansi().a("Use Graphviz to visualize: ").bold()
                .format("`dot -Tpng %s > loom-products.png`", dotFile).boldOff().newline());
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

        for (final TaskInfo task : moduleRunner.describeTasks()) {
            writeLabel(pw, task);
        }

        for (final GoalInfo task : moduleRunner.describeGoals()) {
            writeLabel(pw, task);
        }

        for (final TaskInfo task : moduleRunner.describeTasks()) {
            if (!task.getUsedProducts().isEmpty()) {
                writeEdge(pw, task);
            }
        }

        for (final GoalInfo task : moduleRunner.describeGoals()) {
            if (!task.getUsedProducts().isEmpty()) {
                writeEdge(pw, task);
            }
        }

        pw.println("}");
    }

    private static void writeLabel(final PrintWriter pw, final GoalInfo task) {
        final String label = task.getName();
        pw.printf("    %s [label=\"%s\", color=gold2, shape=tripleoctagon];%n",
            task.getName(), label);
    }

    private static void writeLabel(final PrintWriter pw, final TaskInfo task) {
        pw.printf("    %s [label=\"%s\\n[%s Plugin]\"",
            task.getProvidedProduct(), task.getProvidedProduct(), task.getPluginName());

        if (task.isIntermediateProduct()) {
            pw.print(", color=grey, fontcolor=grey");
        }

        pw.println("];");
    }

    private static void writeEdge(final PrintWriter pw, final TaskInfo task) {
        writeEdge(pw, task.getProvidedProduct(), constructValue(task.getUsedProducts()));
    }

    private static void writeEdge(final PrintWriter pw, final String providedProduct,
                                  final String usedProducts) {
        pw.printf("    %s -> %s;%n", providedProduct, usedProducts);
    }

    private static void writeEdge(final PrintWriter pw, final GoalInfo task) {
        writeEdge(pw, task.getName(), constructValue(task.getUsedProducts()));
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
