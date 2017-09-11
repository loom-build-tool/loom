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

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import builders.loom.plugin.junit.shared.ProgressListenerDelegate;

public class ProgressListenerBridge implements TestExecutionListener {

    private final ProgressListenerDelegate progressListener;

    public ProgressListenerBridge(final ProgressListenerDelegate progressListener) {
        this.progressListener = progressListener;
    }

    public void testPlanExecutionStarted(final TestPlan testPlan) {
        final long totalTests = testPlan.countTestIdentifiers((t) -> true);
        progressListener.total(totalTests);
    }

    public void dynamicTestRegistered(final TestIdentifier testIdentifier) {
        progressListener.test();
    }

    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        progressListener.skip();
    }

    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {

        switch (testExecutionResult.getStatus()) {
            case SUCCESSFUL:
                progressListener.success();
                break;
            case ABORTED:
                progressListener.abort();
                break;
            case FAILED:
                if (isFailure(testExecutionResult)) {
                    progressListener.fail();
                } else {
                    progressListener.error();
                }
                break;
            default:
                throw new IllegalStateException("Unknown status: "
                    + testExecutionResult.getStatus());
        }
    }

    private boolean isFailure(final TestExecutionResult testExecutionResult) {
        return testExecutionResult.getThrowable().isPresent()
            && testExecutionResult.getThrowable().get() instanceof AssertionError;
    }

}
