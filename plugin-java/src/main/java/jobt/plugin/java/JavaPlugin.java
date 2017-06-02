package jobt.plugin.java;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class JavaPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final JavaCompileTask javaCompileTask = new JavaCompileTask(logger, buildConfig, executionContext,
            CompileTarget.MAIN, dependencyResolver);

        final JavaCompileTask testCompileTask = new JavaCompileTask(logger, buildConfig, executionContext,
            CompileTarget.TEST, dependencyResolver);

        final JavaTestTask testTask = new JavaTestTask(logger, testCompileTask.getClassPath());

        taskRegistry.register("compileJava", javaCompileTask);
        taskRegistry.register("compileTestJava", testCompileTask);
        taskRegistry.register("jar", new JavaAssembleTask(buildConfig));
        taskRegistry.register("processResources", new ResourcesTask(CompileTarget.MAIN));
        taskRegistry.register("processTestResources", new ResourcesTask(CompileTarget.TEST));
        taskRegistry.register("test", testTask);
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
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
