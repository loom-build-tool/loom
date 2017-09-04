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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

class LogListener implements TestExecutionListener {

    private static final Logger LOG = Logger.getLogger(LogListener.class.getName());

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        LOG.info("Started testPlan " + testPlan.getRoots());
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        LOG.info("Finished testPlan " + testPlan.getRoots());
    }

    @Override
    public void dynamicTestRegistered(final TestIdentifier testIdentifier) {
        LOG.info("Dynamically registered tst " + testIdentifier.getUniqueId());
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        LOG.info("Skipped test " + testIdentifier.getUniqueId()
            + " - reason: " + reason);
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        LOG.info("Started test " + testIdentifier.getUniqueId());
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {

        if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
            LOG.log(Level.SEVERE, "Test failed: " + testIdentifier.getUniqueId(),
                testExecutionResult.getThrowable());
        } else {
            LOG.info("Finished test " + testIdentifier.getUniqueId());
        }
    }

    @Override
    public void reportingEntryPublished(final TestIdentifier testIdentifier,
                                        final ReportEntry entry) {
        LOG.info("Test " + testIdentifier.getUniqueId() + " reported: " + entry);
    }

}
