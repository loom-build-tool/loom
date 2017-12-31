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

package builders.loom.plugin.maven;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.sonatype.aether.connector.wagon.WagonProvider;

// TODO De-duplicate code (service-maven)
class DefaultWagonProvider implements WagonProvider {

    @Override
    public Wagon lookup(final String roleHint) throws Exception {
        switch (roleHint) {
            case "http":
                return newHttpWagon();
            case "https":
                return newHttpsWagon();
            default:
                return null;
        }
    }

    private Wagon newHttpWagon() {
        return new HttpWagon();
    }

    private Wagon newHttpsWagon() {
        return new HttpWagon();
    }

    @Override
    public void release(final Wagon wagon) {
    }

}
