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

package builders.loom.cli;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class LoomCommand {

    private static final Options OPTIONS = buildOptions();

    private final boolean cleanFlag;
    private final boolean helpFlag;
    private final boolean noCacheFlag;
    private final Map<String, String> systemProperties;
    private final String printProducts;
    private final String release;
    private final List<String> products;

    LoomCommand(final String[] args) {
        try {
            final CommandLine parse = new DefaultParser().parse(OPTIONS, args);
            helpFlag = parse.hasOption("help");
            cleanFlag = parse.hasOption("clean");
            noCacheFlag = parse.hasOption("no-cache");
            release = parse.getOptionValue("release");

            printProducts = !parse.hasOption("products") ? null
                : parse.getOptionValue("products", "text");

            final Map<String, String> sysProps = new LinkedHashMap<>();
            parse.getOptionProperties("D").forEach((key, value) ->
                sysProps.put((String) key, (String) value));
            systemProperties = Collections.unmodifiableMap(sysProps);

            products = Collections.unmodifiableList(parse.getArgList());
        } catch (final ParseException e) {
            throw new IllegalStateException("Error parsing command line: " + e.getMessage());
        }
    }

    boolean isCleanFlag() {
        return cleanFlag;
    }

    boolean isHelpFlag() {
        return helpFlag;
    }

    boolean isNoCacheFlag() {
        return noCacheFlag;
    }

    Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    String getPrintProducts() {
        return printProducts;
    }

    String getRelease() {
        return release;
    }

    List<String> getProducts() {
        return products;
    }

    boolean isAnyOperationRequested() {
        return cleanFlag || printProducts != null || !products.isEmpty();
    }

    private static Options buildOptions() {
        return new Options()
            .addOption("h", "help", false, "Prints this help")
            .addOption("c", "clean", false, "Clean before execution")
            .addOption("n", "no-cache", false,
                "Disable all caches (use on CI servers); also implies clean")
            .addOption(
                Option.builder("r")
                    .longOpt("release")
                    .numberOfArgs(1)
                    .optionalArg(false)
                    .argName("version")
                    .desc("Defines the version to use for artifact creation")
                    .build())
            .addOption(
                Option.builder("p")
                    .longOpt("products")
                    .numberOfArgs(1)
                    .optionalArg(true)
                    .argName("format")
                    .desc("Show available products (formats: text [default], dot)")
                    .build())
            .addOption(
                Option.builder("D")
                    .hasArgs()
                    .valueSeparator('=')
                    .desc("Sets a system property")
                    .build());
    }

    void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("loom [option...] [product|goal...]", OPTIONS);
    }

}
