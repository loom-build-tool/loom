package jobt.api;

import java.util.Set;
import java.util.function.Supplier;

public interface TaskRegistry {

    void registerTask(String taskName, Supplier<Task> taskSupplier, Set<String> providedProducts,
                      Set<String> usedProducts);

    void registerGoal(String goalName, Set<String> usedProducts);

}
