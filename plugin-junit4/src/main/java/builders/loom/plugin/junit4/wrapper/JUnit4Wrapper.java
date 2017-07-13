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

package builders.loom.plugin.junit4.wrapper;

import java.util.List;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import builders.loom.plugin.junit4.shared.TestResult;

/**
 * Wrapper gets injected into final classloader.
 */
public class JUnit4Wrapper {

    public TestResult run(final ClassLoader classLoader, final List<Class<?>> testClasses) {

        Thread.currentThread().setContextClassLoader(classLoader);

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new RunListener() {
            @Override
            public void testFailure(final Failure failure) throws Exception {
                log(failure.toString());
            }

        });
        final Result run = jUnitCore.run(computer, testClasses.toArray(new Class[]{}));


        return new TestResult(
            run.wasSuccessful(), run.getRunCount(), run.getFailureCount(), run.getIgnoreCount());

    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    private static void log(final String message) {
        System.err.println(message);
    }

}
