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

package builders.loom.plugin.mavenresolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressIndicator.class);

    private final String taskDescription;

    public ProgressIndicator(final String taskDescription) {
        this.taskDescription = taskDescription;
    }

    /**
     * Callers should provide a human-readable string stating the progress made so far.
     *
     * Example: "downloaded 5 (of 10 total) artifacts from maven repo central"
     * Full report:
     *  Maven Resolver (running 2,4s): downloaded 5 ....
     */
    public void reportProgress(final String progressMessage) {
        LOG.debug("Got progress message for <{}>: {}",
            taskDescription, progressMessage);
    }

}
