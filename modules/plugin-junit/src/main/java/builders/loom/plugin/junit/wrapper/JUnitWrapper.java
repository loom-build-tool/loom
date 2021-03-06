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

package builders.loom.plugin.junit.wrapper;

import java.nio.file.Path;
import java.util.Set;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import builders.loom.plugin.junit.shared.ProgressListenerDelegate;
import builders.loom.plugin.junit.shared.TestResult;

/**
 * Wrapper gets injected into final classloader.
 */
public class JUnitWrapper {

    public TestResult run(final ClassLoader classLoader, final Path classesDir,
                          final Path reportDir,
                          final ProgressListenerDelegate progressListenerDelegate) {

        Thread.currentThread().setContextClassLoader(classLoader);

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClasspathRoots(Set.of(classesDir)))
            .build();

        final Launcher launcher = LauncherFactory.create();

        final SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        final XmlReportListener xmlReportListener = new XmlReportListener(reportDir);
        launcher.registerTestExecutionListeners(new LogListener(), xmlReportListener,
            new ProgressListenerBridge(progressListenerDelegate), summaryListener);

        launcher.execute(request);

        final TestExecutionSummary summary = summaryListener.getSummary();

        return new TestResult(
            summary.getTimeStarted(),
            summary.getTimeFinished(),
            summary.getTotalFailureCount(),
            summary.getContainersFoundCount(),
            summary.getContainersStartedCount(),
            summary.getContainersSkippedCount(),
            summary.getContainersAbortedCount(),
            summary.getContainersSucceededCount(),
            summary.getContainersFailedCount(),
            summary.getTestsFoundCount(),
            summary.getTestsStartedCount(),
            summary.getTestsSkippedCount(),
            summary.getTestsAbortedCount(),
            summary.getTestsSucceededCount(),
            summary.getTestsFailedCount());
    }

}
