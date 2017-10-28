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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiString;

import builders.loom.api.TaskStatus;
import builders.loom.core.ExecutionStatus;
import builders.loom.core.plugin.TaskType;

final class ExecutionReportPrinter {

    void print(final Map<String, ExecutionStatus> durations) {
        final long totalDuration = durations.values().stream()
            .mapToLong(ExecutionStatus::getDuration)
            .sum();

        AnsiConsole.out().println(Ansi.ansi()
            .newline()
            .bold()
            .a(Ansi.Attribute.UNDERLINE)
            .a("Execution statistics (ordered by product completion time)")
            .reset()
            .newline());

        final Table table = new Table();
        for (final Map.Entry<String, ExecutionStatus> entry : durations.entrySet()) {
            final ExecutionStatus executionStatus = entry.getValue();

            table.add(
                renderType(executionStatus),
                entry.getKey() + " " + renderStatus(executionStatus.getTaskStatus()),
                renderTime(totalDuration, executionStatus.getDuration())
            );
        }

        table.print();
    }

    private String renderType(final ExecutionStatus executionStatus) {
        if (executionStatus.getType() == TaskType.GOAL) {
            return "@|magenta Goal|@";
        }

        return "@|cyan Task|@";
    }

    private String renderStatus(final TaskStatus status) {
        final StringBuilder sb = new StringBuilder("@|");
        switch (status) {
            case DONE:
                sb.append("bold,green");
                break;
            case SKIP:
                sb.append("bold,cyan");
                break;
            case EMPTY:
                sb.append("bold,black");
                break;
            case FAIL:
                sb.append("bold,red");
                break;
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }

        return sb.append(' ').append(status.name()).append("|@").toString();
    }

    private String renderTime(final long totalDuration, final long duration) {
        final double pct = 100D / totalDuration * duration;

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        final double durationSecs = duration / 1_000_000_000D;

        return String.format("%5.2fs (%4.1f%%) %s", durationSecs, pct, durationBar);
    }

    private final class Table {

        private final List<TableRow> rows = new ArrayList<>();
        private final Map<Integer, Integer> columnSizes = new HashMap<>();

        void add(final String... columns) {
            rows.add(new TableRow(this, columns));
        }

        void print() {
            for (final TableRow row : rows) {
                row.print();
            }
        }

        int getColSize(final int col) {
            return columnSizes.computeIfAbsent(col, (c) -> {
                int colSize = 0;
                for (final TableRow row : rows) {
                    final int rowColSize = row.getColSize(col);
                    if (rowColSize > colSize) {
                        colSize = rowColSize;
                    }
                }

                return colSize;
            });
        }

        private final class TableRow {

            private final Table table;
            private final List<String> columns;

            TableRow(final Table table, final String... columns) {
                this.table = table;
                this.columns = new ArrayList<>(columns.length);

                for (final String column : columns) {
                    final Ansi ansi = Ansi.ansi();
                    this.columns.add(ansi.render(column).toString());
                }
            }

            void print() {
                final PrintStream out = AnsiConsole.out();
                final Ansi ansi = Ansi.ansi();
                for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
                    final String column = columns.get(i);
                    ansi.a(column);

                    final int colSize = table.getColSize(i);
                    final int delta = colSize - getColSize(i);
                    if (delta > 0) {
                        ansi.a(String.join("", Collections.nCopies(delta, " ")));
                    }

                    ansi.a(' ');
                }
                out.println(ansi);
            }

            int getColSize(final int col) {
                return new AnsiString(columns.get(col)).length();
            }

        }

    }

}
