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

package builders.loom.cli;

import java.nio.file.Path;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import builders.loom.log.LoomLoggerFactory;

final class LogConfiguration {

    private LogConfiguration() {
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    static void configureLogger(final Path logFile) {
        final LoomLoggerFactory lc = (LoomLoggerFactory) LoggerFactory.getILoggerFactory();
        lc.setLogFile(logFile);
        lc.start();

        // Ensure SLF4J is used (instead of java.util.logging)
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // instead of console outputs
        StdOut2SLF4J.install();
    }

    static void stop() {
        StdOut2SLF4J.uninstall();

        ((LoomLoggerFactory) LoggerFactory.getILoggerFactory()).stop();
    }

}
