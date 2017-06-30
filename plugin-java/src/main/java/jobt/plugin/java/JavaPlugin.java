package jobt.plugin.java;

import jobt.api.AbstractPlugin;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.RuntimeConfiguration;

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
            .uses("source", "compilation")
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
            .impl(() -> new ResourcesTask(runtimeConfiguration, CompileTarget.MAIN))
            .uses("resources")
            .provides("processedResources")
            .register();

        task("processTestResources")
            .impl(() -> new ResourcesTask(runtimeConfiguration, CompileTarget.TEST))
            .provides("processedTestResources")
            .uses("testResources")
            .register();

        task("runTest")
            .impl(JavaTestTask::new)
            .provides("test")
            .uses("testDependencies", "processedResources", "compilation",
                "processedTestResources", "testCompilation")
            .register();

        goal("check")
            .requires("test")
            .register();

        goal("build")
            .requires("jar", "sourcesJar", "test", "check")
            .register();
    }

}
