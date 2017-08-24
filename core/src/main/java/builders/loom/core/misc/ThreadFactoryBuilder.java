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

package builders.loom.core.misc;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadFactoryBuilder {

    private String nameFormat;
    private Boolean daemon;
    private Integer priority;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(final String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public ThreadFactoryBuilder withNameFormat(final String val) {
        this.nameFormat = val;
        return this;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(final Boolean daemon) {
        this.daemon = daemon;
    }

    public ThreadFactoryBuilder withDaemon(final Boolean val) {
        this.daemon = val;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public ThreadFactoryBuilder withPriority(final Integer val) {
        this.priority = val;
        return this;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    public void setUncaughtExceptionHandler(
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public ThreadFactoryBuilder withUncaughtExceptionHandler(
            final Thread.UncaughtExceptionHandler val) {
        this.uncaughtExceptionHandler = val;
        return this;
    }

    public ThreadFactory build() {
        return new CustomThreadFactory(nameFormat, daemon, priority, uncaughtExceptionHandler);
    }

    private static String format(final String format, final Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    private class CustomThreadFactory implements ThreadFactory {

        private final AtomicInteger threadId = new AtomicInteger(0);

        private final String nameFormat;
        private final Boolean daemon;
        private final Integer priority;
        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        CustomThreadFactory(final String nameFormat, final Boolean daemon, final Integer priority,
                            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.nameFormat = nameFormat;
            this.daemon = daemon;
            this.priority = priority;
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r);

            if (nameFormat != null) {
                thread.setName(format(nameFormat, threadId.getAndIncrement()));
            }

            if (daemon != null) {
                thread.setDaemon(daemon);
            }

            if (priority != null) {
                thread.setPriority(priority);
            }

            if (uncaughtExceptionHandler != null) {
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            }

            return thread;
        }

    }

}
