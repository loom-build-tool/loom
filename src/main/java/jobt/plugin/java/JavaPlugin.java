package jobt.plugin.java;

import java.io.IOException;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.MavenResolver;
import jobt.TaskTemplate;
import jobt.config.BuildConfig;
import jobt.plugin.AbstractPlugin;

public class JavaPlugin extends AbstractPlugin {

    public JavaPlugin(final BuildConfig buildConfig, final TaskTemplate taskTemplate) {
        final MavenResolver mavenResolver = new MavenResolver();
        registerTask("compileJava",
            new JavaCompileTask(buildConfig, CompileTarget.MAIN, mavenResolver));
        final JavaCompileTask testCompileTask =
            new JavaCompileTask(buildConfig, CompileTarget.TEST, mavenResolver);
        registerTask("compileTestJava", testCompileTask);
        registerTask("jar", new JavaAssembleTask(buildConfig));
        registerTask("processResources", new ResourcesTask(CompileTarget.MAIN));
        registerTask("processTestResources", new ResourcesTask(CompileTarget.TEST));

        try {
            registerTask("test", new JavaTestTask(testCompileTask.getClassPath()));
        } catch (final DependencyCollectionException | DependencyResolutionException
            | IOException e) {
            throw new IllegalStateException(e);
        }

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
