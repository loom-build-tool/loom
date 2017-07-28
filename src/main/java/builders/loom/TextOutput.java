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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.plugin.GoalInfo;
import builders.loom.plugin.TaskInfo;

public final class TextOutput {

    private TextOutput() {
    }

    public static void generate(final ModuleRunner moduleRunner) {
        AnsiConsole.out().println(
            Ansi.ansi()
                .newline()
                .bold()
                .a("Available products:")
                .reset()
                .newline());

        final List<String> pluginNames = moduleRunner.getPluginNames().stream()
            .sorted()
            .collect(Collectors.toList());

        for (final Iterator<String> iterator = pluginNames.iterator(); iterator.hasNext();) {
            final String pluginName = iterator.next();
            final Collection<TaskInfo> configuredTasks =
                moduleRunner.configuredTasksByPluginName(pluginName);

            AnsiConsole.out().println(
                Ansi.ansi()
                    .a("Plugin ")
                    .bold()
                    .a(pluginName)
                    .reset()
                    .a(":")
            );

            final List<TaskInfo> productTasks = configuredTasks.stream()
                .filter(ct -> ct.getPluginName().equals(pluginName))
                .sorted(Comparator.comparing(TaskInfo::getProvidedProduct))
                .collect(Collectors.toList());

            for (final TaskInfo task : productTasks) {
                final Ansi ansi = Ansi.ansi();

                if (task.isIntermediateProduct()) {
                    ansi.fgBlack().bold();
                } else {
                    ansi.fgYellow();
                }

                ansi.a(task.getProvidedProduct())
                    .reset()
                    .a(" - ")
                    .fgCyan()
                    .a(task.getDescription())
                    .reset();

                AnsiConsole.out().println(ansi);
            }

            if (iterator.hasNext()) {
                AnsiConsole.out().println();
            }
        }

        final List<GoalInfo> goals = moduleRunner.configuredGoals().stream()
            .sorted(Comparator.comparing(GoalInfo::getName))
            .collect(Collectors.toList());

        if (!goals.isEmpty()) {
            AnsiConsole.out().println(
                Ansi.ansi()
                    .newline()
                    .newline()
                    .bold()
                    .a("Available goals:")
                    .reset()
                    .newline());

            for (final GoalInfo goal : goals) {
                AnsiConsole.out().println(
                    Ansi.ansi()
                        .fgYellow()
                        .a(goal.getName())
                        .reset()
                        .a(" - ")
                        .fgCyan()
                        .a("Depends on: ")
                        .fgYellow()
                        .a(goal.getUsedProducts().stream()
                            .sorted()
                            .collect(Collectors.joining(", ")))
                        .reset()
                );
            }
        }

        AnsiConsole.out().println();
    }

}
