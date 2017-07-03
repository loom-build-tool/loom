package jobt.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import jobt.api.service.ServiceLocator;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin<S extends PluginSettings> implements Plugin {

    private final S pluginSettings;
    private TaskRegistry taskRegistry;
    private ServiceLocator serviceLocator;
    private BuildConfig buildConfig;
    private RuntimeConfiguration runtimeConfiguration;

    public AbstractPlugin() {
        this.pluginSettings = null;
    }

    public AbstractPlugin(final S pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public S getPluginSettings() {
        return pluginSettings;
    }

    @Override
    public void setTaskRegistry(final TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @Override
    public void setServiceLocator(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    protected TaskBuilder task(final String taskName) {
        return new TaskBuilder(taskName);
    }

    protected GoalBuilder goal(final String goalName) {
        return new GoalBuilder(goalName);
    }

    protected ServiceBuilder service(final String serviceName) {
        return new ServiceBuilder(serviceName);
    }

    protected class TaskBuilder {

        private final String taskName;
        private Supplier<Task> taskSupplier;
        private Set<String> providedProducts;
        private Set<String> usedProducts = Collections.emptySet();

        public TaskBuilder(final String taskName) {
            this.taskName = taskName;
        }

        public TaskBuilder impl(final Supplier<Task> taskSupplierValue) {
            this.taskSupplier = taskSupplierValue;
            return this;
        }

        public TaskBuilder provides(final String... products) {
            providedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public TaskBuilder uses(final String... products) {
            usedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public void register() {
            Objects.requireNonNull(taskSupplier,
                "taskSupplier missing on task <" + taskName + ">");
            Objects.requireNonNull(providedProducts,
                "providedProducts missing on task <" + taskName + ">");
            Objects.requireNonNull(usedProducts,
                "usedProducts missing on task <" + taskName + ">");
            taskRegistry.registerTask(taskName, taskSupplier, providedProducts, usedProducts);
        }
    }

    protected class GoalBuilder {

        private final String goalName;
        private Set<String> usedProducts = Collections.emptySet();

        public GoalBuilder(final String goalName) {
            this.goalName = goalName;
        }

        public GoalBuilder requires(final String... products) {
            this.usedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public void register() {
            Objects.requireNonNull(usedProducts,
                "usedProducts missing on goal <" + goalName + ">");
            taskRegistry.registerGoal(goalName, usedProducts);
        }

    }

    protected class ServiceBuilder {

        private final String serviceName;
        private Supplier<Service> serviceSupplier;

        public ServiceBuilder(
            final String serviceName) {
            this.serviceName = serviceName;
        }

        public ServiceBuilder impl(final Supplier<Service> supplier) {
            this.serviceSupplier = supplier;
            return this;
        }

        public void register() {
            Objects.requireNonNull(serviceSupplier,
                "serviceSupplier missing on service <" + serviceName + ">");
            serviceLocator.registerService(serviceName, serviceSupplier);
        }

    }

}
