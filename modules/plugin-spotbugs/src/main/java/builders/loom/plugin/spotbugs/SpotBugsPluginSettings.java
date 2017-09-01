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

package builders.loom.plugin.spotbugs;

import builders.loom.api.PluginSettings;

public class SpotBugsPluginSettings implements PluginSettings {

    private String customPlugins;
    private String effort = "default";
    private String reportLevel = "NORMAL";

    public String getCustomPlugins() {
        return customPlugins;
    }

    public void setCustomPlugins(final String customPlugins) {
        this.customPlugins = customPlugins;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(final String effort) {
        this.effort = effort;
    }

    public String getReportLevel() {
        return reportLevel;
    }

    public void setReportLevel(final String reportLevel) {
        this.reportLevel = reportLevel;
    }

}
