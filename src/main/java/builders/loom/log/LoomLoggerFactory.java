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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LoomLoggerFactory implements ILoggerFactory {

    private final LogFilter logFilter = new LogFilter();
    private List<LogAppender> logAppenders;
    private Path logFile;

    @Override
    public Logger getLogger(final String name) {
        return new LoomLogger(name, logAppenders, logFilter);
    }

    public void setLogFile(final Path logFile) {
        this.logFile = logFile;
    }

    public void start() {
        logAppenders = List.of(new ConsoleLogAppender(), new DiskLogAppender(logFile));
    }

    public void stop() {
        for (final LogAppender logAppender : logAppenders) {
            try {
                logAppender.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

}
