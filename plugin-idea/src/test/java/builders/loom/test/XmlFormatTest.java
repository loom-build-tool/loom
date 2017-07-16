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

package builders.loom.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import builders.loom.util.IOUtil;

/**
 * Some details see https://stackoverflow.com/questions/2325388/what-is-the-shortest-way-to-pretty-print-a-org-w3c-dom-document-to-stdout.
 */
public class XmlFormatTest {

    @Test
    public void xmlFormatError() throws Exception {

        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

        try (final InputStream resourceAsStream = readResource("/modules-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Element modules = getOnlyElementByTagName(doc, "modules");

            final Element module = doc.createElement("module");
            module.setAttribute("fileurl", "value1");
            module.setAttribute("filepath", "value2");
            modules.appendChild(module);

            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> clazz = classLoader.loadClass(
                "com.sun.org.apache.xml.internal.serializer.ToXMLStream");
            final Object toXMLStream = clazz.newInstance();

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            clazz.getMethod("setOutputStream", OutputStream.class).invoke(toXMLStream, bos);
            clazz.getMethod("serialize", Node.class).invoke(toXMLStream, doc);

            assertEquals(
                new String(IOUtil.toByteArray(readResource("/modules-poorformat.xml")),
                    StandardCharsets.UTF_8), new String(bos.toByteArray(), StandardCharsets.UTF_8));
        }

    }

    private static Element getOnlyElementByTagName(final Document doc, final String tagName) {
        final NodeList nodeList = doc.getElementsByTagName(tagName);
        return (Element) nodeList.item(0);
    }

    private InputStream readResource(final String resourceName) {
        return new BufferedInputStream(getClass().getResourceAsStream(resourceName));
    }

}
