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

package builders.loom.plugin.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class LoggingBugReporter extends AbstractBugReporter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingBugReporter.class);

    @Override
    protected void doReportBug(final BugInstance bugInstance) {
        LOG.error("Findbugs bug: {}", bugInstance.getMessage());
    }

    @Override
    public void reportAnalysisError(final AnalysisError error) {
        LOG.error("Findbugs analysis error: {}", error);
    }

    @Override
    public void reportMissingClass(final String string) {
        LOG.error("Missing class: {}", string);
    }

    @Override
    public void finish() {

    }

    @Override
    public BugCollection getBugCollection() {
        return null;
    }

    @Override
    public void observeClass(final ClassDescriptor classDescriptor) {

    }

}
