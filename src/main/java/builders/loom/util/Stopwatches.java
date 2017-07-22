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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Stopwatches {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    private static final boolean CPU_TIME_SUPPORTED = THREAD_MX_BEAN.isThreadCpuTimeSupported();
    private static final Map<String, Watch> WATCHES = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> WATCH_NAMES = new ThreadLocal<>();
    private static final AtomicLong TOTAL_DURATION = new AtomicLong();

    private Stopwatches() {
    }

    public static void startProcess(final String name) {
        WATCH_NAMES.set(name);
        if (WATCHES.put(name, new Watch(currentTime())) != null) {
            throw new IllegalStateException("Watch for " + name + " already existed");
        }
    }

    private static long currentTime() {
        return CPU_TIME_SUPPORTED ? THREAD_MX_BEAN.getCurrentThreadCpuTime() : System.nanoTime();
    }

    public static void stopProcess() {
        final String watchName = WATCH_NAMES.get();
        if (watchName == null) {
            throw new IllegalStateException("No watchName registered");
        }
        WATCH_NAMES.remove();

        final Watch watch = WATCHES.get(watchName);
        if (watch == null) {
            throw new IllegalStateException("No watch for " + watchName + " found");
        }
        watch.stop(currentTime());
        TOTAL_DURATION.addAndGet(watch.getDuration());
    }

    public static long getTotalDuration() {
        return TOTAL_DURATION.get();
    }

    public static Map<String, Watch> getWatches() {
        return Collections.unmodifiableMap(WATCHES);
    }

}
