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

package builders.loom.util;

public final class Stopwatch {

    private static final int MS_IN_NANOS = 1_000_000;
    private long start;

    public Stopwatch() {
        this(System.nanoTime());
    }

    public Stopwatch(final long start) {
        this.start = start;
    }

    public long elapsedNanos() {
        return System.nanoTime() - start;
    }

    public void reset() {
        start = System.nanoTime();
    }

    @Override
    public String toString() {
        return String.format("%d ms", elapsedNanos() / MS_IN_NANOS);
    }

}
