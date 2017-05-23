package jobt.plugin.java;

import jobt.TaskTemplate;
import jobt.config.BuildConfig;
import jobt.plugin.AbstractPlugin;

public class JavaPlugin extends AbstractPlugin {

    public JavaPlugin(final BuildConfig buildConfig, final TaskTemplate taskTemplate) {
        registerTask("compileJava", new JavaCompileTask(buildConfig, CompileTarget.MAIN));
        registerTask("compileTestJava", new JavaCompileTask(buildConfig, CompileTarget.TEST));
        registerTask("jar", new JavaAssembleTask(buildConfig));
        registerTask("processResources", new ResourcesTask(CompileTarget.MAIN));
        registerTask("processTestResources", new ResourcesTask(CompileTarget.TEST));

        taskTemplate.task("compileJava");

        taskTemplate.task("processResources");

        taskTemplate.task("classes").dependsOn(
            taskTemplate.task("compileJava"),
            taskTemplate.task("processResources"));

        taskTemplate.task("javadoc").dependsOn(
            taskTemplate.task("classes"));

        taskTemplate.task("compileTestJava").dependsOn(
            taskTemplate.task("classes"));

        taskTemplate.task("processTestResources");

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
