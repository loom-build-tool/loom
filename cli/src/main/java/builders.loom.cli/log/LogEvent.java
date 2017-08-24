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

package builders.loom.cli.log;

import java.time.LocalDateTime;

import org.slf4j.Marker;
import org.slf4j.event.Level;

public final class LogEvent {

    private final LocalDateTime occurred = LocalDateTime.now();
    private final Level level;
    private final String name;
    private final String threadName;
    private final String message;
    private final Throwable throwable;
    private final Marker marker;

    public LogEvent(final Level level, final String name,
                    final String threadName, final String message,
                    final Throwable throwable, final Marker marker) {
        this.level = level;
        this.name = name;
        this.threadName = threadName;
        this.message = message;
        this.throwable = throwable;
        this.marker = marker;
    }

    public LocalDateTime getOccurred() {
        return occurred;
    }

    public Level getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Marker getMarker() {
        return marker;
    }

}
