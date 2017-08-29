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

package builders.loom.plugin.java;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.slf4j.Logger;

public class DiagnosticLogListener implements DiagnosticListener<JavaFileObject> {

    private final Logger log;

    DiagnosticLogListener(final Logger log) {
        this.log = log;
    }

    @Override
    public void report(final Diagnostic<? extends JavaFileObject> diagnostic) {
        switch (diagnostic.getKind()) {
            case ERROR:
                log.error(diagnostic.toString());
                break;
            case WARNING:
            case MANDATORY_WARNING:
                log.warn(diagnostic.toString());
                break;
            case NOTE:
                log.info(diagnostic.toString());
                break;
            default:
                log.debug(diagnostic.toString());
        }
    }

}
