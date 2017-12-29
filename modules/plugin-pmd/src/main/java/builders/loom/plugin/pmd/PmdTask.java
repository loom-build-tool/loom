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

package builders.loom.plugin.pmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.api.RepositoryPathAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ManagedGenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.IOUtil;
import builders.loom.util.ProductChecksumUtil;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.RulesetsFactoryUtils;
import net.sourceforge.pmd.ThreadSafeReportListener;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.AbstractRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.stat.Metric;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class PmdTask extends AbstractModuleTask implements RepositoryPathAware {

    private static final Logger LOG = LoggerFactory.getLogger(PmdTask.class);

    private final CompileTarget compileTarget;

    private final PmdPluginSettings pluginSettings;
    private final String sourceProductId;
    private final String reportOutputDescription;
    private Path repositoryPath;

    public PmdTask(final PmdPluginSettings pluginSettings,
                   final CompileTarget compileTarget) {
        this.pluginSettings = pluginSettings;
        this.compileTarget = compileTarget;

        switch (compileTarget) {
            case MAIN:
                sourceProductId = "source";
                reportOutputDescription = "PMD main report";
                break;
            case TEST:
                sourceProductId = "testSource";
                reportOutputDescription = "PMD test report";
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget: " + compileTarget);
        }
    }

    @Override
    public void setRepositoryPath(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    private PMDConfiguration getConfiguration() {
        final PMDConfiguration configuration = new PMDConfiguration();
        configuration.setReportShortNames(true);
        configuration.setRuleSets(parseRuleSetSetting());
        configuration.setRuleSetFactoryCompatibilityEnabled(false);
        configuration.setSourceEncoding("UTF-8");
        configuration.setThreads(0);
        configuration.setMinimumPriority(RulePriority.valueOf(pluginSettings.getMinimumPriority()));
        configuration.setAnalysisCacheLocation(
            resolveCacheFile().toAbsolutePath().normalize().toString());
        configuration.setDefaultLanguageVersion(getLanguageVersion(getModuleConfig()));

        if (configuration.getSuppressMarker() != null) {
            LOG.debug("Configured suppress marker: {}", configuration.getSuppressMarker());
        }
        return configuration;
    }

    private String parseRuleSetSetting() {
        final String ruleSets = pluginSettings.getRuleSets();
        if (ruleSets == null) {
            return null;
        }

        final List<String> ruleSetFiles = new ArrayList<>();
        for (final String set : ruleSets.split(",")) {
            final Path p = getBuildContext().getPath().resolve(set);
            if (Files.exists(p)) {
                ruleSetFiles.add(p.toAbsolutePath().normalize().toString());
            } else {
                // Might be classloader relative...
                ruleSetFiles.add(set);
            }
        }

        return String.join(",", ruleSetFiles);
    }

    private LanguageVersion getLanguageVersion(final ModuleBuildConfig buildConfig) {
        final String version = buildConfig.getBuildSettings()
            .getJavaPlatformVersion().getStringVersion();

        final LanguageVersion languageVersion = LanguageRegistry
            .findLanguageVersionByTerseName("java " + version);

        if (languageVersion == null) {
            throw new IllegalStateException("Unsupported java version:" + version);
        }

        return languageVersion;
    }

    private Path resolveCacheFile() {
        return repositoryPath
            .resolve(getBuildContext().getModuleName())
            .resolve(compileTarget.name().toLowerCase())
            .resolve("pmd.cache");
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<Product> sourceTreeProduct =
            useProduct(sourceProductId, Product.class);

        if (!sourceTreeProduct.isPresent()) {
            return TaskResult.empty();
        }

        final PMDConfiguration configuration = getConfiguration();
        final RuleSetFactory ruleSetFactory = buildRuleSetFactory(configuration);

        final RuleContext ctx = new RuleContext();
        final AtomicInteger ruleViolations = new AtomicInteger(0);
        ctx.getReport().addListener(new LogListener(ruleViolations));

        final Path srcDir = Paths.get(sourceTreeProduct.get().getProperty("srcDir"));

        final List<DataSource> files = Files
            .find(srcDir, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())
            .map(p -> new FileDataSource(p.toFile()))
            .collect(Collectors.toList());

        LOG.info("PMD will analyze {} files", files.size());

        final String inputPaths = sourceTreeProduct.get().getProperty("srcDir");
        configuration.setInputPaths(inputPaths);

        final Path reportDir =
            FileUtil.createOrCleanDirectory(resolveReportDir("pmd", compileTarget));

        final HTMLRenderer htmlRenderer = buildHtmlRenderer(reportDir);
        final List<Renderer> renderers = Arrays.asList(new LogRenderer(inputPaths), htmlRenderer);

        for (final Renderer renderer : renderers) {
            renderer.start();
        }

        PMD.processFiles(configuration, ruleSetFactory, files, ctx,
            renderers);

        for (final Renderer renderer : renderers) {
            renderer.end();
            IOUtil.closeQuietly(renderer.getWriter());
        }

        final int ruleViolationCnt = ruleViolations.get();

        if (ruleViolationCnt > 0) {
            return TaskResult.fail(newProduct(reportDir, reportOutputDescription),
                "Stopping build since PMD found " + ruleViolationCnt
                    + " rule violations in the code");
        }

        return TaskResult.done(newProduct(reportDir, reportOutputDescription));
    }

    private HTMLRenderer buildHtmlRenderer(final Path reportPath) throws IOException {
        final Path reportDir = Files.createDirectories(reportPath);

        final HTMLRenderer htmlRenderer = new HTMLRenderer();
        final String reportFileName = compileTarget.name().toLowerCase() + ".html";
        final Path reportFile = reportDir.resolve(reportFileName);
        htmlRenderer.setWriter(Files.newBufferedWriter(reportFile));
        return htmlRenderer;
    }

    private RuleSetFactory buildRuleSetFactory(final PMDConfiguration configuration) {
        final RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(configuration);

        if (LOG.isDebugEnabled()) {
            final RuleSets ruleSets;
            try {
                ruleSets = ruleSetFactory.createRuleSets(configuration.getRuleSets());
            } catch (final RuleSetNotFoundException e) {
                throw new IllegalStateException(e);
            }

            for (final RuleSet ruleSet : ruleSets.getAllRuleSets()) {
                final List<String> ruleNames = ruleSet.getRules().stream()
                    .map(Rule::getName)
                    .collect(Collectors.toList());

                LOG.debug("Using ruleSets from ruleset {} ({}): {}",
                    ruleSet.getName(), configuration.getRuleSets(), ruleNames);
            }
        }

        return ruleSetFactory;
    }

    private static Product newProduct(final Path reportDir, final String outputInfo) {
        return new ManagedGenericProduct("reportDir", reportDir.toString(),
            ProductChecksumUtil.recursiveMetaChecksum(reportDir),
            new OutputInfo(outputInfo, reportDir));
    }

    private static class LogRenderer extends AbstractRenderer {

        private final String inputPaths;

        LogRenderer(final String inputPaths) {
            super("log", "Logging renderer");
            this.inputPaths = inputPaths;
        }

        @Override
        public void start() {
        }

        @Override
        public void startFileAnalysis(final DataSource dataSource) {
            LOG.debug("Processing file {}", dataSource.getNiceFileName(false, inputPaths));
        }

        @Override
        public void renderFileReport(final Report r) {
        }

        @Override
        public void end() {
        }

        @Override
        public String defaultFileExtension() {
            return null;
        }

    }

    private static class LogListener implements ThreadSafeReportListener {

        private final AtomicInteger ruleViolations;

        LogListener(final AtomicInteger ruleViolations) {
            this.ruleViolations = ruleViolations;
        }

        @Override
        public void ruleViolationAdded(final RuleViolation ruleViolation) {
            LOG.warn("{}:{} violates rule {} - {}", ruleViolation.getFilename(),
                ruleViolation.getBeginLine(), ruleViolation.getRule().getName(),
                ruleViolation.getDescription());

            ruleViolations.incrementAndGet();
        }

        @Override
        public void metricAdded(final Metric metric) {
        }

    }

}
