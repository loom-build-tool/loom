package jobt.plugin.java;

import java.io.IOException;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.MavenResolver;
import jobt.plugin.AbstractPlugin;
import jobt.plugin.CompileTarget;
import jobt.plugin.TaskRegistry;
import jobt.plugin.TaskTemplate;

public class JavaPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final MavenResolver mavenResolver = new MavenResolver();

        final JavaCompileTask javaCompileTask =
            new JavaCompileTask(buildConfig, executionContext, CompileTarget.MAIN, mavenResolver);

        final JavaCompileTask testCompileTask =
            new JavaCompileTask(buildConfig, executionContext, CompileTarget.TEST, mavenResolver);

        final JavaTestTask testTask;
        try {
            testTask = new JavaTestTask(testCompileTask.getClassPath());
        } catch (final DependencyCollectionException | DependencyResolutionException
            | IOException e) {
            throw new IllegalStateException(e);
        }

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
