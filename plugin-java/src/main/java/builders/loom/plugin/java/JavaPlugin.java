package builders.loom.plugin.java;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.BuildConfig;
import builders.loom.api.CompileTarget;
import builders.loom.api.RuntimeConfiguration;

public class JavaPlugin extends AbstractPlugin<JavaPluginSettings> {

    public JavaPlugin() {
        super(new JavaPluginSettings());
    }

    @Override
    public void configure() {
        final BuildConfig buildConfig = getBuildConfig();
        final RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration();

        task("provideSource")
            .impl(() -> new JavaProvideSourceDirTask(CompileTarget.MAIN))
            .provides("source")
            .register();

        task("provideTestSource")
            .impl(() -> new JavaProvideSourceDirTask(CompileTarget.TEST))
            .provides("testSource")
            .register();

        task("compileJava")
            .impl(() -> new JavaCompileTask(buildConfig, runtimeConfiguration, CompileTarget.MAIN))
            .provides("compilation")
            .uses("source", "compileDependencies")
            .register();

        task("compileTestJava")
            .impl(() -> new JavaCompileTask(buildConfig, runtimeConfiguration, CompileTarget.TEST))
            .provides("testCompilation")
            .uses("compilation", "testSource", "testDependencies")
            .register();

        task("assemble")
            .impl(() -> new JavaAssembleTask(buildConfig, getPluginSettings()))
            .provides("jar", "sourcesJar")
            .uses("source", "resources", "processedResources", "compilation")
            .register();

        task("provideResources")
            .impl(() -> new JavaProvideResourcesDirTask(CompileTarget.MAIN))
            .provides("resources")
            .register();

        task("provideTestResources")
            .impl(() -> new JavaProvideResourcesDirTask(CompileTarget.TEST))
            .provides("testResources")
            .register();

        task("processResources")
            .impl(() -> new ResourcesTask(runtimeConfiguration, buildConfig, getPluginSettings(),
                CompileTarget.MAIN))
            .provides("processedResources")
            .uses("resources")
            .register();

        task("processTestResources")
            .impl(() -> new ResourcesTask(runtimeConfiguration, buildConfig, getPluginSettings(),
                CompileTarget.TEST))
            .provides("processedTestResources")
            .uses("testResources")
            .register();

        goal("check")
            .register();

        goal("build")
            .requires("jar", "sourcesJar", "check")
            .register();
    }

}
