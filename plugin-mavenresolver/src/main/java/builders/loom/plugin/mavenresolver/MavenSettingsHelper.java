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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import builders.loom.util.StringReplaceUtil;

class MavenSettingsHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MavenInstallModuleTask.class);

    private final DocumentBuilder documentBuilder;

    MavenSettingsHelper() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    Path findLocalMavenRepository() {
        // ${user.home}/.m2/settings.xml
        final Optional<Path> pathFromUserSettings = getPath(
            Paths.get(System.getProperty("user.home"), ".m2", "settings.xml"));
        if (pathFromUserSettings.isPresent()) {
            return pathFromUserSettings.get();
        }

        // ${maven.home}/conf/settings.xml
        final Optional<Path> pathFromGlobalSettings = getPath(
            Paths.get(System.getenv("M2_HOME"), "conf", "settings.xml"));
        if (pathFromGlobalSettings.isPresent()) {
            return pathFromGlobalSettings.get();
        }

        // default
        final Path repository = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        LOG.debug("Use default repository location: {}", repository);
        return repository;
    }

    private Optional<Path> getPath(final Path settingsFile) {
        if (Files.notExists(settingsFile)) {
            return Optional.empty();
        }

        final Optional<Path> optPath = readLocalRepository(settingsFile);
        optPath.ifPresent(path -> LOG.debug("Found Maven repository in {}: {}",
            settingsFile, path));

        return optPath;
    }

    private Optional<Path> readLocalRepository(final Path file) {
        final Document document = parseFile(file);

        final NodeList localRepositoryTag = document.getElementsByTagName("localRepository");
        if (localRepositoryTag.getLength() == 0) {
            return Optional.empty();
        }

        if (localRepositoryTag.getLength() > 1) {
            throw new IllegalStateException("Found multiple localRepository definitions in "
                + file);
        }

        return Optional.of(Paths.get(replaceVars(localRepositoryTag.item(0).getTextContent())));
    }

    private String replaceVars(final String str) {
        return StringReplaceUtil.replaceString(str, placeholder -> {
            if (placeholder.startsWith("env.")) {
                return Optional.ofNullable(System.getenv(placeholder.substring("env.".length())));
            }

            return Optional.ofNullable(System.getProperty(placeholder));
        });
    }

    private Document parseFile(final Path file) {
        try {
            return documentBuilder.parse(file.toFile());
        } catch (final IOException | SAXException e) {
            throw new IllegalStateException("Error parsing " + file, e);
        }
    }

}
