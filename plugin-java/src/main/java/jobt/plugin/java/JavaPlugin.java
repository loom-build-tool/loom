package jobt.plugin.java;

import jobt.api.AbstractPlugin;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.RuntimeConfiguration;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class JavaPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final BuildConfig buildConfig = getBuildConfig();
        final RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration();

        taskRegistry.register("provideSource",
            new JavaProvideSourceDirTask(buildConfig, CompileTarget.MAIN,
            uses(), provides("source"))
            );

        taskRegistry.register("provideTestSource",
            new JavaProvideSourceDirTask(buildConfig, CompileTarget.TEST,
            uses(), provides("testSource"))
            );

        taskRegistry.register("compileJava", new JavaCompileTask(buildConfig,
            runtimeConfiguration, CompileTarget.MAIN,
            uses("source", "compileDependencies"), provides("compilation")));

        taskRegistry.register("compileTestJava", new JavaCompileTask(buildConfig,
            runtimeConfiguration, CompileTarget.TEST,
            uses("compilation", "testSource", "testDependencies"), provides("testCompilation")));

        taskRegistry.register("jar", new JavaAssembleTask(buildConfig));

        taskRegistry.register("provideResources",
            new JavaProvideResourcesDirTask(buildConfig, CompileTarget.MAIN,
                uses(), provides("resources")));

        taskRegistry.register("provideTestResources",
            new JavaProvideResourcesDirTask(buildConfig, CompileTarget.TEST,
                uses(), provides("testResources")));

        taskRegistry.register("processResources",
            new ResourcesTask(runtimeConfiguration, CompileTarget.MAIN,
                uses("resources"), provides("processedResources")));

        taskRegistry.register("processTestResources",
            new ResourcesTask(runtimeConfiguration, CompileTarget.TEST,
                uses("testResources"), provides("processedTestResources")));

        taskRegistry.register("test", new JavaTestTask(uses("testDependencies"), provides()));
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("compileJava")
            .dependsOn(
                taskTemplate.task("provideSource"),
                taskTemplate.task("resolveCompileDependencies"));

        taskTemplate.task("processResources")
            .dependsOn(
                taskTemplate.task("provideResources"));

        taskTemplate.task("classes").dependsOn(
            taskTemplate.task("compileJava"),
            taskTemplate.task("processResources"));

        taskTemplate.task("javadoc").dependsOn(
            taskTemplate.task("classes"));

        taskTemplate.task("compileTestJava").dependsOn(
            taskTemplate.task("provideTestSource"),
            taskTemplate.task("resolveTestDependencies"),
            taskTemplate.task("classes"));

        taskTemplate.task("processTestResources")
            .dependsOn(
                taskTemplate.task("provideTestResources"));

        taskTemplate.task("testClasses").dependsOn(
            taskTemplate.task("compileTestJava"),
            taskTemplate.task("processTestResources"));

        taskTemplate.task("test").dependsOn(
            taskTemplate.task("classes"),
            taskTemplate.task("testClasses"));

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("test"));

        taskTemplate.task("jar").dependsOn(
            taskTemplate.task("classes"));

        taskTemplate.task("uploadArchives").dependsOn(
            taskTemplate.task("jar"));

        taskTemplate.task("assemble").dependsOn(
            taskTemplate.task("jar"));

        taskTemplate.task("build").dependsOn(
            taskTemplate.task("check"),
            taskTemplate.task("assemble"));
    }

}
