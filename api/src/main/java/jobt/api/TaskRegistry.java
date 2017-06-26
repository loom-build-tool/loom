package jobt.api;

public interface TaskRegistry {

    void register(String name, Task task, ProvidedProducts providedProducts);

}
