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
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ReportProduct;
import builders.loom.api.product.SourceTreeProduct;
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
public class PmdModuleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(PmdModuleTask.class);

    private final CompileTarget compileTarget;

    private final Path cacheDir;
    private final PmdPluginSettings pluginSettings;

    public PmdModuleTask(final PmdPluginSettings pluginSettings,
                         final CompileTarget compileTarget, final Path cacheDir) {
        this.pluginSettings = pluginSettings;

        // Ensure SLF4J is used (instead of java.util.logging)
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        this.compileTarget = compileTarget;
        this.cacheDir = cacheDir;

    }

    private PMDConfiguration getConfiguration() {
        final PMDConfiguration configuration = new PMDConfiguration();
        configuration.setReportShortNames(true);
        configuration.setRuleSets(pluginSettings.getRuleSets());
        configuration.setRuleSetFactoryCompatibilityEnabled(false);
        configuration.setSourceEncoding("UTF-8");
        configuration.setThreads(0);
        configuration.setMinimumPriority(RulePriority.valueOf(pluginSettings.getMinimumPriority()));
        configuration.setAnalysisCacheLocation(resolveCacheFile().toString());
        configuration.setDefaultLanguageVersion(getLanguageVersion(getModuleConfig()));

        if (configuration.getSuppressMarker() != null) {
            LOG.debug("Configured suppress marker: {}", configuration.getSuppressMarker());
        }
        return configuration;
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
        return cacheDir.resolve(Paths.get("pmd",
            String.format("%s.cache", compileTarget.name().toLowerCase())));
    }

    @Override
    public TaskResult run() throws Exception {
        final PMDConfiguration configuration = getConfiguration();
        final RuleSetFactory ruleSetFactory = buildRuleSetFactory(configuration);

        final RuleContext ctx = new RuleContext();
        final AtomicInteger ruleViolations = new AtomicInteger(0);
        ctx.getReport().addListener(new LogListener(ruleViolations));

        final Optional<SourceTreeProduct> sourceTreeProduct = getSource();

        if (!sourceTreeProduct.isPresent()) {
            return completeEmpty();
        }

        final Path srcDir = sourceTreeProduct.get().getSrcDir();

        final List<DataSource> files = sourceTreeProduct.get().getSourceFiles().stream()
            .map(Path::toFile)
            .map(FileDataSource::new)
            .collect(Collectors.toList());

        final String inputPaths = srcDir.toString();
        configuration.setInputPaths(inputPaths);

        final Path reportPath = LoomPaths.reportDir(getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(), "pmd")
            .resolve(compileTarget.name().toLowerCase());

        final HTMLRenderer htmlRenderer = buildHtmlRenderer(reportPath);
        final List<Renderer> renderers = Arrays.asList(new LogRenderer(inputPaths), htmlRenderer);

        for (final Renderer renderer : renderers) {
            renderer.start();
        }

        PMD.processFiles(configuration, ruleSetFactory, files, ctx,
            renderers);

        for (final Renderer renderer : renderers) {
            renderer.end();
        }

        final int ruleViolationCnt = ruleViolations.get();

        LOG.debug("{} problems found", ruleViolationCnt);

        if (ruleViolationCnt > 0) {
            throw new IllegalStateException("Stopping build since PMD found " + ruleViolationCnt
                + " rule violations in the code");
        }

        return completeOk(product(reportPath));
    }

    private HTMLRenderer buildHtmlRenderer(final Path reportPath) throws IOException {
        final Path reportDir = Files.createDirectories(reportPath);

        final HTMLRenderer htmlRenderer = new HTMLRenderer();
        final String reportFileName = compileTarget.name().toLowerCase() + ".html";
        final Path reportFile = reportDir.resolve(reportFileName);
        htmlRenderer.setWriter(Files.newBufferedWriter(reportFile,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
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

    private Optional<SourceTreeProduct> getSource() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("source", SourceTreeProduct.class);
            case TEST:
                return useProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException("Unknown compileTarget: " + compileTarget);
        }
    }

    private ReportProduct product(final Path reportPath) {
        switch (compileTarget) {
            case MAIN:
                return new ReportProduct(reportPath, "PMD main report");
            case TEST:
                return new ReportProduct(reportPath, "PMD test report");
            default:
                throw new IllegalStateException("Unknown compileTarget: " + compileTarget);
        }
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
