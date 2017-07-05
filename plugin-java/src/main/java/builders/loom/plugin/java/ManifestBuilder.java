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

import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestBuilder {

    private final Attributes mainAttributes;

    public ManifestBuilder(final Manifest manifest) {
        mainAttributes = manifest.getMainAttributes();
    }

    public ManifestBuilder put(final Attributes.Name attr, final String value) {
        mainAttributes.put(attr, value);
        return this;
    }

    public ManifestBuilder put(final String attr, final String value) {
        mainAttributes.put(new Attributes.Name(attr), value);
        return this;
    }

}
