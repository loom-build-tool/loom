package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jobt.api.AbstractTask;
import jobt.api.DependencyScope;
import jobt.api.TaskStatus;
import jobt.api.product.ClasspathProduct;

public class MavenPluginResolverTask extends AbstractTask {

    private final String dependenciesToProvide;
    private final Set<String> taskDependencies;
    private final MavenResolver mavenResolver;

    public MavenPluginResolverTask(final String dependenciesToProvide,
                                   final Set<String> taskDependencies) {

        this.dependenciesToProvide = dependenciesToProvide;
        this.taskDependencies = taskDependencies;
        this.mavenResolver = MavenResolverSingleton.getInstance();
    }

    @Override
    public TaskStatus run() throws Exception {
        final List<String> dependencies = new ArrayList<>(taskDependencies);
        getProvidedProducts().complete(dependenciesToProvide,
            new ClasspathProduct(mavenResolver.resolve(dependencies,
                DependencyScope.COMPILE, dependenciesToProvide)));

        return TaskStatus.OK;
    }

}
