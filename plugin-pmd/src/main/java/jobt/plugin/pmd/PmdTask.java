package jobt.plugin.pmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.JobtPaths;
import jobt.api.TaskStatus;
import jobt.api.product.ReportProduct;
import jobt.api.product.SourceTreeProduct;
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
public class PmdTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(PmdTask.class);

    private final CompileTarget compileTarget;
    private final PMDConfiguration configuration = new PMDConfiguration();

    public PmdTask(final BuildConfig buildConfig, final PmdPluginSettings pluginSettings,
                   final CompileTarget compileTarget) {

        // Ensure SLF4J is used (instead of java.util.logging)
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        this.compileTarget = compileTarget;

        configuration.setReportShortNames(true);
        configuration.setRuleSets(pluginSettings.getRuleSets());
        configuration.setRuleSetFactoryCompatibilityEnabled(false);
        configuration.setSourceEncoding("UTF-8");
        configuration.setThreads(0);
        configuration.setMinimumPriority(RulePriority.valueOf(pluginSettings.getMinimumPriority()));
        configuration.setAnalysisCacheLocation(resolveCacheFile().toString());
        configuration.setDefaultLanguageVersion(getLanguageVersion(buildConfig));

        if (configuration.getSuppressMarker() != null) {
            LOG.debug("Configured suppress marker: {}", configuration.getSuppressMarker());
        }
    }

    private LanguageVersion getLanguageVersion(final BuildConfig buildConfig) {
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
        return Paths.get(".jobt/", "cache", "pmd",
            String.format("%s.cache", compileTarget.name().toLowerCase()));
    }

    @Override
    public TaskStatus run() throws Exception {
        final RuleSetFactory ruleSetFactory = buildRuleSetFactory();

        final RuleContext ctx = new RuleContext();
        final AtomicInteger ruleViolations = new AtomicInteger(0);
        ctx.getReport().addListener(new LogListener(ruleViolations));

        final Path srcDir = getSource();
        final List<DataSource> files = Files.walk(srcDir)
            .filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".java"))
            .map(f -> new FileDataSource(f.toFile()))
            .collect(Collectors.toList());

        final String inputPaths = srcDir.toString();
        configuration.setInputPaths(inputPaths);

        final HTMLRenderer htmlRenderer = buildHtmlRenderer();
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

        return complete(TaskStatus.OK);
    }

    private HTMLRenderer buildHtmlRenderer() throws IOException {
        final Path reportDir = Files.createDirectories(JobtPaths.REPORT_PATH.resolve("pmd"));

        final HTMLRenderer htmlRenderer = new HTMLRenderer();
        final String reportFileName = compileTarget.name().toLowerCase() + ".html";
        final Path reportFile = reportDir.resolve(reportFileName);
        htmlRenderer.setWriter(Files.newBufferedWriter(reportFile,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        return htmlRenderer;
    }

    private RuleSetFactory buildRuleSetFactory() {
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

    private Path getSource() {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("source", SourceTreeProduct.class)
                    .getSrcDir();
            case TEST:
                return getUsedProducts().readProduct("testSource", SourceTreeProduct.class)
                    .getSrcDir();
            default:
                throw new IllegalStateException("Unknown compileTarget: " + compileTarget);
        }
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("pmdMainReport",
                    new ReportProduct(JobtPaths.REPORT_PATH.resolve("pmd")));
                break;
            case TEST:
                getProvidedProducts().complete("pmdTestReport",
                    new ReportProduct(JobtPaths.REPORT_PATH.resolve("pmd")));
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget: " + compileTarget);
        }
        return status;
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
            LOG.error("{}:{} violates rule {} - {}", ruleViolation.getFilename(),
                ruleViolation.getBeginLine(), ruleViolation.getRule().getName(),
                ruleViolation.getDescription());

            ruleViolations.incrementAndGet();
        }

        @Override
        public void metricAdded(final Metric metric) {
        }

    }

}
