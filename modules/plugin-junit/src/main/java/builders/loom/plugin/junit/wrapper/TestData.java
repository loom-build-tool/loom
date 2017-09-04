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

import java.time.Duration;
import java.time.Instant;

final class TestData {

    private Instant startedAt;
    private Instant endedAt;
    private TestStatus status;
    private Throwable throwable;
    private String skipReason;

    private TestData() {
    }

    static TestData skip(final String reason) {
        final TestData testData = new TestData();
        testData.skipReason = reason;
        return testData;
    }

    static TestData start(final Instant now) {
        final TestData testData = new TestData();
        testData.startedAt = now;
        return testData;
    }

    void testFinished(final Instant finishedAt, final TestStatus finishedStatus,
                      final Throwable finishedThrowable) {
        endedAt = finishedAt;
        status = finishedStatus;
        throwable = finishedThrowable;
    }

    Instant getStartedAt() {
        return startedAt;
    }

    Instant getEndedAt() {
        return endedAt;
    }

    Duration getDuration() {
        if (startedAt == null || endedAt == null) {
            return Duration.ZERO;
        }

        return Duration.between(startedAt, endedAt);
    }

    TestStatus getStatus() {
        return status;
    }

    Throwable getThrowable() {
        return throwable;
    }

    String getSkipReason() {
        return skipReason;
    }

}
