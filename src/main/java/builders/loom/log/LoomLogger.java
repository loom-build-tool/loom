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

package builders.loom.log;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@SuppressWarnings("checkstyle:overloadmethodsdeclarationorder")
public class LoomLogger implements Logger {

    private final String name;
    private final List<LogAppender> logAppenders;
    private final LogFilter logFilter;

    LoomLogger(final String name, final List<LogAppender> logAppenders,
               final LogFilter logFilter) {
        this.name = name;
        this.logAppenders = logAppenders;
        this.logFilter = logFilter;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return logFilter.isEnabled(name, Level.TRACE);
    }

    @Override
    public void trace(final String msg) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, msg);
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arg);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arguments);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, msg, t);
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return logFilter.isEnabled(name, Level.TRACE, marker);
    }

    @Override
    public void trace(final Marker marker, final String msg) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, msg);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object arg) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, arg);
    }

    @Override
    public void trace(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object... argArray) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, argArray);
    }

    @Override
    public void trace(final Marker marker, final String msg, final Throwable t) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logFilter.isEnabled(name, Level.DEBUG);
    }

    @Override
    public void debug(final String msg) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, msg);
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arg);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, msg, t);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return logFilter.isEnabled(name, Level.DEBUG, marker);
    }

    @Override
    public void debug(final Marker marker, final String msg) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, msg);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object arg) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arg);
    }

    @Override
    public void debug(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object... arguments) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(final Marker marker, final String msg, final Throwable t) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logFilter.isEnabled(name, Level.INFO);
    }

    @Override
    public void info(final String msg) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, msg);
    }

    @Override
    public void info(final String format, final Object arg) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arg);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arguments);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, msg, t);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return logFilter.isEnabled(name, Level.INFO, marker);
    }

    @Override
    public void info(final Marker marker, final String msg) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, msg);
    }

    @Override
    public void info(final Marker marker, final String format, final Object arg) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arg);
    }

    @Override
    public void info(final Marker marker, final String format,
                     final Object arg1, final Object arg2) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(final Marker marker, final String format, final Object... arguments) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arguments);
    }

    @Override
    public void info(final Marker marker, final String msg, final Throwable t) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logFilter.isEnabled(name, Level.WARN);
    }

    @Override
    public void warn(final String msg) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, msg);
    }

    @Override
    public void warn(final String format, final Object arg) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arg);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arguments);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, msg, t);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return logFilter.isEnabled(name, Level.WARN, marker);
    }

    @Override
    public void warn(final Marker marker, final String msg) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, msg);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object arg) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arg);
    }

    @Override
    public void warn(final Marker marker, final String format,
                     final Object arg1, final Object arg2) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object... arguments) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arguments);
    }

    @Override
    public void warn(final Marker marker, final String msg, final Throwable t) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logFilter.isEnabled(name, Level.ERROR);
    }

    @Override
    public void error(final String msg) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, msg);
    }

    @Override
    public void error(final String format, final Object arg) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arg);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arguments);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, msg, t);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return logFilter.isEnabled(name, Level.ERROR, marker);
    }

    @Override
    public void error(final Marker marker, final String msg) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, msg);
    }

    @Override
    public void error(final Marker marker, final String format, final Object arg) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arg);
    }

    @Override
    public void error(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(final Marker marker, final String format, final Object... arguments) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arguments);
    }

    @Override
    public void error(final Marker marker, final String msg, final Throwable t) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, msg, t);
    }

    private void append(final Level level, final String msg) {
        append(new LogEvent(level, name, Thread.currentThread().getName(), msg, null));
    }

    private void append(final Level level, final String format, final Object arg) {
        final FormattingTuple tuple = MessageFormatter.format(format, arg);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable()));
    }

    private void append(final Level level, final String format,
                        final Object arg1, final Object arg2) {
        final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable()));
    }

    private void append(final Level level, final String format, final Object[] arguments) {
        final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable()));
    }

    private void append(final LogEvent message) {
        logAppenders.forEach(logAppender -> logAppender.append(message));
    }

}
