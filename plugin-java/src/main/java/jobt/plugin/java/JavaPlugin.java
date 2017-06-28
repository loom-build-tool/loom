package jobt.plugin.java;

import jobt.api.AbstractPlugin;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.RuntimeConfiguration;

public class JavaPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        final BuildConfig buildConfig = getBuildConfig();
        final RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration();

        task("provideSource")
            .impl(() -> new JavaProvideSourceDirTask(buildConfig, CompileTarget.MAIN))
            .provides("source")
            .register();

        task("provideTestSource")
            .impl(() -> new JavaProvideSourceDirTask(buildConfig, CompileTarget.TEST))
            .provides("testSource")
            .register();

        task("compileJava")
            .impl(() -> new JavaCompileTask(buildConfig, runtimeConfiguration, CompileTarget.MAIN))
            .uses("source", "compileDependencies")
            .provides("compilation")
            .register();

        task("compileTestJava")
            .impl(() -> new JavaCompileTask(buildConfig, runtimeConfiguration, CompileTarget.TEST))
            .uses("compilation", "testSource", "testDependencies")
            .provides("testCompilation")
            .register();

        task("assemble")
            .impl(() -> new JavaAssembleTask(buildConfig))
            .uses("source", "compilation")
            .provides("jar", "sourcesJar")
            .register();

        task("provideResources")
            .impl(() -> new JavaProvideResourcesDirTask(buildConfig, CompileTarget.MAIN))
            .provides("resources")
            .register();

        task("provideTestResources")
            .impl(() -> new JavaProvideResourcesDirTask(buildConfig, CompileTarget.TEST))
            .provides("testResources")
            .register();

        task("processResources")
            .impl(() -> new ResourcesTask(runtimeConfiguration, CompileTarget.MAIN))
            .uses("resources")
            .provides("processedResources")
            .register();

        task("processTestResources")
            .impl(() -> new ResourcesTask(runtimeConfiguration, CompileTarget.TEST))
            .uses("testResources")
            .provides("processedTestResources")
            .register();

        task("runTest")
            .impl(JavaTestTask::new)
            .uses("testDependencies", "processedResources", "compilation", "processedTestResources", "testCompilation")
            .provides("test")
            .register();

        goal("check")
            .register();

        goal("build")
            .requires("jar", "sourcesJar", "test", "check")
            .register();
    }

}
