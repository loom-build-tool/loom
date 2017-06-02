package jobt.plugin.checkstyle;

import java.io.IOException;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.MavenResolver;
import jobt.TaskTemplate;
import jobt.config.BuildConfig;
import jobt.plugin.AbstractPlugin;
import jobt.plugin.java.CompileTarget;
import jobt.plugin.java.JavaCompileTask;

public class CheckstylePlugin extends AbstractPlugin {

    public CheckstylePlugin(final BuildConfig buildConfig, final TaskTemplate taskTemplate) {
        final MavenResolver mavenResolver = new MavenResolver();
        final JavaCompileTask javaCompileTask =
            new JavaCompileTask(buildConfig, CompileTarget.MAIN, mavenResolver);
        final JavaCompileTask javaTestCompileTask =
            new JavaCompileTask(buildConfig, CompileTarget.TEST, mavenResolver);

        try {
            registerTask("checkstyleMain",
                new CheckstyleTask("src/main/java", javaCompileTask.getClassPath()));
        } catch (final DependencyCollectionException | DependencyResolutionException
            | IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            registerTask("checkstyleTest",
                new CheckstyleTask("src/test/java", javaTestCompileTask.getClassPath()));
        } catch (final DependencyCollectionException | DependencyResolutionException
            | IOException e) {
            throw new IllegalStateException(e);
        }

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("checkstyleMain"));

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("checkstyleTest"));
    }

}
