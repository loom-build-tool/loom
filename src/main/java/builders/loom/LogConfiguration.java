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

package builders.loom;

import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import builders.loom.log.LoomLoggerFactory;
import builders.loom.log.StdOut2SLF4J;

public final class LogConfiguration {

    private LogConfiguration() {
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public static void configureLogger(final Path logFile) {
        final LoomLoggerFactory lc = (LoomLoggerFactory) LoggerFactory.getILoggerFactory();
        lc.setLogFile(logFile);
        lc.start();

        StdOut2SLF4J.install();
    }

    public static void stop() {
        StdOut2SLF4J.uninstall();

        ((LoomLoggerFactory) LoggerFactory.getILoggerFactory()).stop();
    }

}
