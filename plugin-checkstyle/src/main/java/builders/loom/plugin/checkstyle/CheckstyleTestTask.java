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

package builders.loom.plugin.checkstyle;

import java.nio.file.Path;

import builders.loom.api.product.ReportProduct;

public class CheckstyleTestTask extends CheckstyleTask {

    public CheckstyleTestTask(final CheckstylePluginSettings pluginSettings, final Path cacheDir) {
        super(pluginSettings, cacheDir);
    }

    @Override
    protected String getSourceTreeProductName() {
        return "testSource";
    }

    @Override
    protected String targetName() {
        return "test";
    }

    @Override
    protected ReportProduct product(final Path reportBaseDir) {
        return new ReportProduct(reportBaseDir, "Checkstyle test report");
    }

    @Override
    protected String classPathProductName() {
        return "testDependencies";
    }

}
