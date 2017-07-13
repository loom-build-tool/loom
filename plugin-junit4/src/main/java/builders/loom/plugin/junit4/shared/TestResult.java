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

package builders.loom.plugin.junit4.shared;

public final class TestResult {

    private final boolean successful;
    private final int runCount;
    private final int failureCount;
    private final int ignoreCount;

    public TestResult(
        final boolean successful,
        final int runCount,
        final int failureCount,
        final int ignoreCount) {
        this.successful = successful;
        this.runCount = runCount;
        this.failureCount = failureCount;
        this.ignoreCount = ignoreCount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public int getRunCount() {
        return runCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getIgnoreCount() {
        return ignoreCount;
    }

    @Override
    public String toString() {
        if (successful) {
            return String.format("%d tests were executed successfully", runCount);
        } else {
            return String.format(
                "Failed after running %d tests,"
                + " where %d tests failed and %d were ignored",
                runCount, failureCount, ignoreCount);

        }
    }
}
