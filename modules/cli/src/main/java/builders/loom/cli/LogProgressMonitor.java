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
import java.util.concurrent.atomic.AtomicInteger;

import builders.loom.core.ProgressMonitor;

public class LogProgressMonitor implements ProgressMonitor {

    private static final AtomicInteger TASKS = new AtomicInteger();
    private static final AtomicInteger COMPLETED_TASKS = new AtomicInteger();

    private final PrintStream out;

    LogProgressMonitor(final PrintStream out) {
        this.out = out;
    }

    @Override
    public void setTasks(final int tasks) {
        TASKS.set(tasks);
    }

    @Override
    public void progress(final String jobName) {
        out.printf("Completed task %d/%d: %s%n",
            COMPLETED_TASKS.incrementAndGet(), TASKS.get(), jobName);
    }

    @Override
    public void progressDownloadedFiles(final String resourceName) {
        out.println("Download " + resourceName);
    }

}
