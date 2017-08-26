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

package builders.loom.cli;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.core.ModuleRunner;
import builders.loom.core.plugin.GoalInfo;
import builders.loom.core.plugin.TaskInfo;

final class TextOutput {

    private TextOutput() {
    }

    static void generate(final ModuleRunner moduleRunner) {
        AnsiConsole.out().println(
            Ansi.ansi()
                .newline()
                .bold()
                .a(Ansi.Attribute.UNDERLINE)
                .a("Available products")
                .reset()
                .newline());

        final List<String> pluginNames = moduleRunner.getPluginNames().stream()
            .sorted()
            .collect(Collectors.toList());



        for (final Iterator<String> iterator = pluginNames.iterator(); iterator.hasNext();) {
            final String pluginName = iterator.next();
            final Collection<TaskInfo> configuredTasks =
                moduleRunner.describePluginTasks(pluginName);

            AnsiConsole.out().println(
                Ansi.ansi()
                    .bold()
                    .a("Plugin ")
                    .fgBrightBlue().a(pluginName).fgDefault()
                    .reset()
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
                    ansi.fgCyan();
                }

                ansi.a(task.getProvidedProduct())
                    .reset()
                    .a(" - ")
                    .a(task.getDescription())
                    .reset();

                AnsiConsole.out().println(ansi);
            }

            if (iterator.hasNext()) {
                AnsiConsole.out().println();
            }
        }

        final List<GoalInfo> goals = moduleRunner.describeGoals().stream()
            .sorted(Comparator.comparing(GoalInfo::getName))
            .collect(Collectors.toList());

        if (!goals.isEmpty()) {
            AnsiConsole.out().println(
                Ansi.ansi()
                    .newline()
                    .bold()
                    .a(Ansi.Attribute.UNDERLINE)
                    .a("Available goals")
                    .reset()
                    .newline());

            for (final GoalInfo goal : goals) {
                final Ansi a = Ansi.ansi()
                    .fgMagenta()
                    .a(goal.getName())
                    .reset()
                    .a(" - ");

                a.a(buildDependsOn(moduleRunner, goal));

                AnsiConsole.out().println(a);
            }
        }

        AnsiConsole.out().println();
    }

    private static Ansi buildDependsOn(final ModuleRunner moduleRunner, final GoalInfo goal) {
        final Ansi a = Ansi.ansi();

        final Set<String> usedProducts = goal.getUsedProducts();
        if (usedProducts.isEmpty()) {
            a.a("No plugin registered a dependency");
        } else {
            a.a("Depends on: ");

            for (final Iterator<String> it = usedProducts.iterator(); it.hasNext();) {
                final String product = it.next();

                final boolean isGoal = moduleRunner.describeGoals().stream()
                    .anyMatch(s -> s.getName().equals(product));

                if (isGoal) {
                    a.fgMagenta();
                } else {
                    a.fgCyan();
                }

                a.a(product);

                if (it.hasNext()) {
                    a.fgDefault().a(", ");
                }
            }
        }

        return a;
    }

}
