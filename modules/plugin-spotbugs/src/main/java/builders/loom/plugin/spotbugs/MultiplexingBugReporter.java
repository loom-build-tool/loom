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

package builders.loom.plugin.spotbugs;

import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugReporterObserver;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class MultiplexingBugReporter implements BugReporter {

    private final List<BugReporter> bugReporters;
    private final ProjectStats projectStats = new ProjectStats();

    public MultiplexingBugReporter(final BugReporter... bugReporters) {
        this.bugReporters = Arrays.asList(bugReporters);
    }

    @Override
    public void setErrorVerbosity(final int level) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.setErrorVerbosity(level);
        }
    }

    @Override
    public void setPriorityThreshold(final int threshold) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.setPriorityThreshold(threshold);
        }
    }

    @Override
    public void reportBug(final BugInstance bugInstance) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.reportBug(bugInstance);
        }
    }

    @Override
    public void finish() {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.finish();
        }
    }

    @Override
    public void reportQueuedErrors() {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.reportQueuedErrors();
        }
    }

    @Override
    public void addObserver(final BugReporterObserver observer) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.addObserver(observer);
        }
    }

    @Override
    public ProjectStats getProjectStats() {
        return projectStats;
    }

    @Override
    public BugCollection getBugCollection() {
        return null;
    }

    @Override
    public void observeClass(final ClassDescriptor classDescriptor) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.observeClass(classDescriptor);
        }
    }

    @Override
    public void reportMissingClass(final ClassNotFoundException ex) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.reportMissingClass(ex);
        }
    }

    @Override
    public void reportMissingClass(final ClassDescriptor classDescriptor) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.reportMissingClass(classDescriptor);
        }
    }

    @Override
    public void logError(final String message) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.logError(message);
        }
    }

    @Override
    public void logError(final String message, final Throwable e) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.logError(message, e);
        }
    }

    @Override
    public void reportSkippedAnalysis(final MethodDescriptor method) {
        for (final BugReporter bugReporter : bugReporters) {
            bugReporter.reportSkippedAnalysis(method);
        }
    }

}
