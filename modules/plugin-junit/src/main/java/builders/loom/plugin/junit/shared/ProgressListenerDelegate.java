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

package builders.loom.plugin.junit.shared;

import builders.loom.api.TestProgressEmitter;

public class ProgressListenerDelegate {

    private final TestProgressEmitter emitter;

    public ProgressListenerDelegate(final TestProgressEmitter emitter) {
        this.emitter = emitter;
    }

    public void total(final long tests) {
        emitter.total(tests);
    }

    public void test() {
        emitter.test();
    }

    public void success() {
        emitter.success();
    }

    public void abort() {
        emitter.abort();
    }

    public void skip() {
        emitter.skip();
    }

    public void fail() {
        emitter.fail();
    }

    public void error() {
        emitter.error();
    }

}
