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

package builders.loom.core;

import builders.loom.api.TestProgressEmitter;

public class TestProgressEmitterBridge implements TestProgressEmitter {

    private final ProgressMonitor progressMonitor;

    TestProgressEmitterBridge(final ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    @Override
    public void total(final long tests) {
        progressMonitor.testsTotal(tests);
    }

    @Override
    public void test() {
        progressMonitor.testsAdd();
    }

    @Override
    public void success() {
        progressMonitor.testSuccess();
    }

    @Override
    public void abort() {
        progressMonitor.testAbort();
    }

    @Override
    public void skip() {
        progressMonitor.testSkip();
    }

    @Override
    public void fail() {
        progressMonitor.testFail();
    }

    @Override
    public void error() {
        progressMonitor.testError();
    }

}
