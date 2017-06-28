package jobt.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin implements Plugin {

    private TaskRegistry taskRegistry;
    private BuildConfig buildConfig;
    private RuntimeConfiguration runtimeConfiguration;

    @Override
    public void setTaskRegistry(final TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
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

    protected class TaskBuilder {

        private final String taskName;
        private Supplier<Task> taskSupplier;
        private Set<String> providedProducts;
        private Set<String> usedProducts = Collections.emptySet();

        public TaskBuilder(final String taskName) {
            this.taskName = taskName;
        }

        public TaskBuilder impl(final Supplier<Task> taskSupplier) {
            this.taskSupplier = taskSupplier;
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
            taskRegistry.registerGoal(goalName, usedProducts);
        }

    }

}
