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
    private TaskTemplate taskTemplate;
    private BuildConfig buildConfig;
    private RuntimeConfiguration runtimeConfiguration;
    private ProductRepository productRepository;

    @Override
    public void setTaskRegistry(final TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @Override
    public void setTaskTemplate(final TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
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

    @Override
    public void setProductRepository(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public final ProvidedProducts provides(final String... productIdLists) {
        return new ProvidedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), productRepository);
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
        private Set<String> usedProducts;
        private Set<String> providedProducts;

        public TaskBuilder(final String taskName) {
            this.taskName = Objects.requireNonNull(taskName);
        }

        public TaskBuilder impl(final Supplier<Task> taskSupplier) {
            this.taskSupplier = taskSupplier;
            return this;
        }

        public TaskBuilder uses(final String... products) {
            usedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public TaskBuilder provides(final String... products) {
            providedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public void register() {
            taskRegistry.registerOnce(taskName, taskSupplier.get(),
                new ProvidedProducts(providedProducts, productRepository));

            final TaskGraphNode task = taskTemplate.task(taskName);

            if (usedProducts != null) {
                final ProductGraphNode[] productGraphNodes = usedProducts.stream()
                    .map(taskTemplate::product)
                    .toArray(ProductGraphNode[]::new);

                task.uses(productGraphNodes);
            }
        }

    }

    protected class GoalBuilder {

        private final String goalName;
        private Set<String> usedProducts;

        public GoalBuilder(final String goalName) {
            this.goalName = Objects.requireNonNull(goalName);
        }

        public GoalBuilder requires(final String... products) {
            this.usedProducts = new HashSet<>(Arrays.asList(Objects.requireNonNull(products)));
            return this;
        }

        public void register() {
            taskRegistry.register(goalName, new WaitForAllProductsTask(),
                new ProvidedProducts(Collections.emptySet(), productRepository));

            final TaskGraphNode taskGraphNode = taskTemplate.virtualProduct(goalName);

            if (usedProducts != null) {
                final ProductGraphNode[] productGraphNodes = usedProducts.stream()
                    .map(taskTemplate::product)
                    .toArray(ProductGraphNode[]::new);

                taskGraphNode.uses(productGraphNodes);
            }
        }

    }

}
