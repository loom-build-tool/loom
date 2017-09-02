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

    private final long timeStarted;
    private final long timeFinished;
    private final long totalFailureCount;
    private final long containersFoundCount;
    private final long containersStartedCount;
    private final long containersSkippedCount;
    private final long containersAbortedCount;
    private final long containersSucceededCount;
    private final long containersFailedCount;
    private final long testsFoundCount;
    private final long testsStartedCount;
    private final long testsSkippedCount;
    private final long testsAbortedCount;
    private final long testsSucceededCount;
    private final long testsFailedCount;

    @SuppressWarnings("checkstyle:parameternumber")
    public TestResult(final long timeStarted, final long timeFinished,
                      final long totalFailureCount, final long containersFoundCount,
                      final long containersStartedCount, final long containersSkippedCount,
                      final long containersAbortedCount, final long containersSucceededCount,
                      final long containersFailedCount, final long testsFoundCount,
                      final long testsStartedCount, final long testsSkippedCount,
                      final long testsAbortedCount, final long testsSucceededCount,
                      final long testsFailedCount) {
        this.timeStarted = timeStarted;
        this.timeFinished = timeFinished;
        this.totalFailureCount = totalFailureCount;
        this.containersFoundCount = containersFoundCount;
        this.containersStartedCount = containersStartedCount;
        this.containersSkippedCount = containersSkippedCount;
        this.containersAbortedCount = containersAbortedCount;
        this.containersSucceededCount = containersSucceededCount;
        this.containersFailedCount = containersFailedCount;
        this.testsFoundCount = testsFoundCount;
        this.testsStartedCount = testsStartedCount;
        this.testsSkippedCount = testsSkippedCount;
        this.testsAbortedCount = testsAbortedCount;
        this.testsSucceededCount = testsSucceededCount;
        this.testsFailedCount = testsFailedCount;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public long getTimeFinished() {
        return timeFinished;
    }

    public long getTotalFailureCount() {
        return totalFailureCount;
    }

    public long getContainersFoundCount() {
        return containersFoundCount;
    }

    public long getContainersStartedCount() {
        return containersStartedCount;
    }

    public long getContainersSkippedCount() {
        return containersSkippedCount;
    }

    public long getContainersAbortedCount() {
        return containersAbortedCount;
    }

    public long getContainersSucceededCount() {
        return containersSucceededCount;
    }

    public long getContainersFailedCount() {
        return containersFailedCount;
    }

    public long getTestsFoundCount() {
        return testsFoundCount;
    }

    public long getTestsStartedCount() {
        return testsStartedCount;
    }

    public long getTestsSkippedCount() {
        return testsSkippedCount;
    }

    public long getTestsAbortedCount() {
        return testsAbortedCount;
    }

    public long getTestsSucceededCount() {
        return testsSucceededCount;
    }

    public long getTestsFailedCount() {
        return testsFailedCount;
    }

    @Override
    public String toString() {
        return "TestResult{"
            + "timeStarted=" + timeStarted
            + ", timeFinished=" + timeFinished
            + ", totalFailureCount=" + totalFailureCount
            + ", containersFoundCount=" + containersFoundCount
            + ", containersStartedCount=" + containersStartedCount
            + ", containersSkippedCount=" + containersSkippedCount
            + ", containersAbortedCount=" + containersAbortedCount
            + ", containersSucceededCount=" + containersSucceededCount
            + ", containersFailedCount=" + containersFailedCount
            + ", testsFoundCount=" + testsFoundCount
            + ", testsStartedCount=" + testsStartedCount
            + ", testsSkippedCount=" + testsSkippedCount
            + ", testsAbortedCount=" + testsAbortedCount
            + ", testsSucceededCount=" + testsSucceededCount
            + ", testsFailedCount=" + testsFailedCount
            + '}';
    }

}
