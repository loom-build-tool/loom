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

class TestData {

    private final Instant startedAt = Instant.now();
    private Instant endedAt;
    private TestStatus status;
    private Throwable throwable;
    private String skipReason;

    void testFinished(final TestStatus status, final Throwable throwable) {
        endedAt = Instant.now();
        this.status = status;
        this.throwable = throwable;
    }

    void testSkipped(final String skipReason) {
        testFinished(TestStatus.SKIPPED, null);
        this.skipReason = skipReason;
    }

    Instant getStartedAt() {
        return startedAt;
    }

    Instant getEndedAt() {
        return endedAt;
    }

    Duration getDuration() {
        return Duration.between(startedAt, endedAt);
    }

    TestStatus getStatus() {
        return status;
    }

    Throwable getThrowable() {
        return throwable;
    }

    public String getSkipReason() {
        return skipReason;
    }

}
