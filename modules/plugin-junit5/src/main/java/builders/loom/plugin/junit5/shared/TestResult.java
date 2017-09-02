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

package builders.loom.plugin.junit5.shared;

public final class TestResult {

    private final boolean successful;
    private final long runCount;
    private final long failureCount;
    private final long ignoreCount;

    public TestResult(
        final boolean successful,
        final long runCount,
        final long failureCount,
        final long ignoreCount) {
        this.successful = successful;
        this.runCount = runCount;
        this.failureCount = failureCount;
        this.ignoreCount = ignoreCount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public long getRunCount() {
        return runCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getIgnoreCount() {
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
