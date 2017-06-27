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
            new JavaProvideSourceDirTask(buildConfig, CompileTarget.MAIN), provides("source")
            );

        taskRegistry.register("provideTestSource",
            new JavaProvideSourceDirTask(buildConfig, CompileTarget.TEST), provides("testSource")
            );

        taskRegistry.register("compileJava", new JavaCompileTask(buildConfig,
            runtimeConfiguration, CompileTarget.MAIN), provides("compilation"));

        taskRegistry.register("compileTestJava", new JavaCompileTask(buildConfig,
            runtimeConfiguration, CompileTarget.TEST), provides("testCompilation"));

        taskRegistry.register("assemble", new JavaAssembleTask(buildConfig), provides("jar", "sourcesJar"));

        taskRegistry.register("provideResources",
            new JavaProvideResourcesDirTask(buildConfig, CompileTarget.MAIN), provides("resources"));

        taskRegistry.register("provideTestResources",
            new JavaProvideResourcesDirTask(buildConfig, CompileTarget.TEST), provides("testResources"));

        taskRegistry.register("processResources",
            new ResourcesTask(runtimeConfiguration, CompileTarget.MAIN), provides("processedResources"));

        taskRegistry.register("processTestResources",
            new ResourcesTask(runtimeConfiguration, CompileTarget.TEST), provides("processedTestResources"));

        taskRegistry.register("runTest", new JavaTestTask(), provides("test"));

        taskRegistry.register("check", new WaitForAllProductsTask(), provides());

        taskRegistry.register("build", new WaitForAllProductsTask(), provides());
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("compileJava")
            .uses(
                taskTemplate.product("source"),
                taskTemplate.product("compileDependencies"));

        taskTemplate.task("processResources")
            .uses(
                taskTemplate.product("resources"));


        taskTemplate.virtualProduct("classes")
            .uses(
                taskTemplate.product("processedResources"),
                taskTemplate.product("compilation"));

//        taskTemplate.task("classes").dependsOn(
//            taskTemplate.task("compileJava"),
//            taskTemplate.task("processResources"));

        // TODO
//        taskTemplate.task("javadoc").uses(
//            taskTemplate.product("classes"));

        taskTemplate.task("compileTestJava").uses(
            taskTemplate.product("compilation"),
            taskTemplate.product("testSource"),
            taskTemplate.product("testDependencies"));

        taskTemplate.task("processTestResources")
            .uses(
                taskTemplate.product("testResources"));

        taskTemplate.virtualProduct("testClasses")
            .uses(
                taskTemplate.product("processedTestResources"),
                taskTemplate.product("testCompilation"));

//        taskTemplate.task("testClasses").dependsOn(
//            taskTemplate.task("compileTestJava"),
//            taskTemplate.task("processTestResources"));

        taskTemplate.task("runTest").uses(
            taskTemplate.product("processedResources"),
            taskTemplate.product("compilation"),
            taskTemplate.product("testClasses"),
            taskTemplate.product("processedTestResources")
            );

        taskTemplate.virtualProduct("check")
            .uses();
//        taskTemplate.task("check").dependsOn(
//            taskTemplate.task("test"));

        // TODO add product for jar
        taskTemplate.task("assemble").uses(
            taskTemplate.product("source"),
            taskTemplate.product("compilation")
            );

        // TODO add product
//        taskTemplate.task("uploadArchives").dependsOn(
//            taskTemplate.task("jar"));

        // TODO add product
//        taskTemplate.task("assemble").dependsOn(
//            taskTemplate.task("jar"));

        taskTemplate.virtualProduct("build").uses(
            taskTemplate.product("jar"),
            taskTemplate.product("sourcesJar"),
            taskTemplate.product("test"),
            taskTemplate.product("check")
            );
    }

}
