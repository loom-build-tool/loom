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

package builders.loom.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SystemUtilTest {

    @Test
    public void fullPath() {
        assertEquals("java", SystemUtil.getFileExtension("/path/to/Klasse.java"));
    }

    @Test
    public void singleFile() {
        assertEquals("java", SystemUtil.getFileExtension("Klasse.java"));
    }

    @Test
    public void pathWithDots() {
        assertEquals("java", SystemUtil.getFileExtension("../path/to/../pack/Klasse.java"));
    }

    @Test
    public void extensionOnly() {
        assertEquals("java", SystemUtil.getFileExtension(".java"));
    }

    @Test
    public void twoDotJava() {
        assertEquals("java", SystemUtil.getFileExtension("/my.java/path/Klasse.java"));
    }

    @Test
    public void failEmpty() {
        SystemUtil.getFileExtension("");
    }

    @Test
    public void nullName() {
        assertNull(SystemUtil.getFileExtension(null));
    }

    @Test
    public void failSepAfterExt() {
        assertEquals("", SystemUtil.getFileExtension("/my.java/more"));
    }

    @Test
    public void windowsPath() {
        assertEquals("java", SystemUtil.getFileExtension("C:\\path\\to\\Klasse.java"));
    }

}
