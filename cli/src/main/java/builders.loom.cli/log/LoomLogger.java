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
        append(Level.TRACE, msg, null);
    }

    @Override
    public void trace(final String format, final Object arg) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arg, null);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arg1, arg2, null);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, format, arguments, null);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        if (!isTraceEnabled()) {
            return;
        }
        append(Level.TRACE, msg, t, null);
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
        append(Level.TRACE, msg, marker);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object arg) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, arg, marker);
    }

    @Override
    public void trace(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, arg1, arg2, marker);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object... argArray) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, format, argArray, marker);
    }

    @Override
    public void trace(final Marker marker, final String msg, final Throwable t) {
        if (!isTraceEnabled(marker)) {
            return;
        }
        append(Level.TRACE, msg, t, marker);
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
        append(Level.DEBUG, msg, null);
    }

    @Override
    public void debug(final String format, final Object arg) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arg, null);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arg1, arg2, null);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, format, arguments, null);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        if (!isDebugEnabled()) {
            return;
        }
        append(Level.DEBUG, msg, t, null);
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
        append(Level.DEBUG, msg, marker);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object arg) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arg, marker);
    }

    @Override
    public void debug(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arg1, arg2, marker);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object... arguments) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, format, arguments, marker);
    }

    @Override
    public void debug(final Marker marker, final String msg, final Throwable t) {
        if (!isDebugEnabled(marker)) {
            return;
        }
        append(Level.DEBUG, msg, t, marker);
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
        append(Level.INFO, msg, null);
    }

    @Override
    public void info(final String format, final Object arg) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arg, null);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arg1, arg2, null);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, format, arguments, null);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        if (!isInfoEnabled()) {
            return;
        }
        append(Level.INFO, msg, t, null);
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
        append(Level.INFO, msg, marker);
    }

    @Override
    public void info(final Marker marker, final String format, final Object arg) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arg, marker);
    }

    @Override
    public void info(final Marker marker, final String format,
                     final Object arg1, final Object arg2) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arg1, arg2, marker);
    }

    @Override
    public void info(final Marker marker, final String format, final Object... arguments) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, format, arguments, marker);
    }

    @Override
    public void info(final Marker marker, final String msg, final Throwable t) {
        if (!isInfoEnabled(marker)) {
            return;
        }
        append(Level.INFO, msg, t, marker);
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
        append(Level.WARN, msg, null);
    }

    @Override
    public void warn(final String format, final Object arg) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arg, null);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arguments, null);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, format, arg1, arg2, null);
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        append(Level.WARN, msg, t, null);
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
        append(Level.WARN, msg, marker);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object arg) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arg, marker);
    }

    @Override
    public void warn(final Marker marker, final String format,
                     final Object arg1, final Object arg2) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arg1, arg2, marker);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object... arguments) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, format, arguments, marker);
    }

    @Override
    public void warn(final Marker marker, final String msg, final Throwable t) {
        if (!isWarnEnabled(marker)) {
            return;
        }
        append(Level.WARN, msg, t, marker);
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
        append(Level.ERROR, msg, null);
    }

    @Override
    public void error(final String format, final Object arg) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arg, null);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arg1, arg2, null);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, format, arguments, null);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        append(Level.ERROR, msg, t, null);
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
        append(Level.ERROR, msg, marker);
    }

    @Override
    public void error(final Marker marker, final String format, final Object arg) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arg, marker);
    }

    @Override
    public void error(final Marker marker, final String format,
                      final Object arg1, final Object arg2) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arg1, arg2, marker);
    }

    @Override
    public void error(final Marker marker, final String format, final Object... arguments) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, format, arguments, marker);
    }

    @Override
    public void error(final Marker marker, final String msg, final Throwable t) {
        if (!isErrorEnabled(marker)) {
            return;
        }
        append(Level.ERROR, msg, t, marker);
    }

    private void append(final Level level, final String msg, final Marker marker) {
        append(new LogEvent(level, name, Thread.currentThread().getName(), msg, null, marker));
    }

    private void append(final Level level, final String format, final Object arg,
                        final Marker marker) {
        final FormattingTuple tuple = MessageFormatter.format(format, arg);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable(), marker));
    }

    private void append(final Level level, final String format,
                        final Object arg1, final Object arg2,
                        final Marker marker) {
        final FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable(), marker));
    }

    private void append(final Level level, final String format, final Object[] arguments,
                        final Marker marker) {
        final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
        append(new LogEvent(level, name, Thread.currentThread().getName(),
            tuple.getMessage(), tuple.getThrowable(), marker));
    }

    private void append(final LogEvent message) {
        logAppenders.forEach(logAppender -> logAppender.append(message));
    }

}
