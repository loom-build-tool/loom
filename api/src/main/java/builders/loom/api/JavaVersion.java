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

package builders.loom.api;

public enum JavaVersion {

    JAVA_1_5(5, "1.5"),
    JAVA_1_6(6, "1.6"),
    JAVA_1_7(7, "1.7"),
    JAVA_1_8(8, "1.8"),
    JAVA_9(9, "9");

    private final int numericVersion;
    private final String stringVersion;

    JavaVersion(final int numericVersion, final String stringVersion) {
        this.numericVersion = numericVersion;
        this.stringVersion = stringVersion;
    }

    public int getNumericVersion() {
        return numericVersion;
    }

    public String getStringVersion() {
        return stringVersion;
    }

    @SuppressWarnings("checkstyle:returncount")
    public static JavaVersion ofVersion(final String versionStr) {
        switch (versionStr) {
            case "9":
                return JAVA_9;
            case "1.8":
            case "8":
                return JAVA_1_8;
            case "1.7":
            case "7":
                return JAVA_1_7;
            case "1.6":
            case "6":
                return JAVA_1_6;
            case "1.5":
            case "5":
                return JAVA_1_5;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + versionStr);
        }
    }

    public static JavaVersion current() {
        return ofVersion(Integer.toString(Runtime.version().major()));
    }

}
