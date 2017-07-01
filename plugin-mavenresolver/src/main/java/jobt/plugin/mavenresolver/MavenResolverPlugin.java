package jobt.plugin.mavenresolver;

import java.util.Set;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;
import jobt.api.PluginSettings;

public class MavenResolverPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig()))
            .provides("compileDependencies")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, getBuildConfig()))
            .provides("testDependencies")
            .register();
    }

    @Override
    public void requestDependency(final String taskName, final Set<String> taskDependencies) {
        task("resolvePluginDependencies." + taskName)
            .impl(() -> new MavenPluginResolverTask("pluginDependencies."
                + taskName, taskDependencies))
            .provides("pluginDependencies." + taskName)
            .register();
    }

}
