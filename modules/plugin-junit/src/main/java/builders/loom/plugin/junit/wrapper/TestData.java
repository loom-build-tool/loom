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

import org.junit.platform.engine.TestExecutionResult;

public class TestData {

    private final String name;
    private final Instant startedAt;
    private Instant endedAt;
    private TestExecutionResult.Status status;
    private Throwable throwable;

    public TestData(final String name) {
        this.name = name;
        startedAt = Instant.now();
    }

    public void testFinished(final TestExecutionResult.Status status, final Throwable throwable) {
        endedAt = Instant.now();
        this.status = status;
        this.throwable = throwable;
    }

    public void testSkipped(final String reason) {
        endedAt = Instant.now();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Duration getDuration() {
        return Duration.between(startedAt, endedAt);
    }

}
