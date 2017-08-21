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

package builders.loom.util.xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class XmlParser {

    private final DocumentBuilder documentBuilder;

    private XmlParser(final DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    public static XmlParser createXmlParser() {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder;
        try {
            documentBuilder = dbFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        return new XmlParser(documentBuilder);

    }

    public Document parse(final Path xmlFile) {
        Objects.requireNonNull(xmlFile);
        try {
            return documentBuilder.parse(xmlFile.toFile());
        } catch (final IOException e) {
            throw new UncheckedIOException("Error reading file " + xmlFile, e);
        } catch (final SAXException e) {
            throw new IllegalStateException("Error parsing file " + xmlFile, e);
        }
    }

}
