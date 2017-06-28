package jobt.api;

public interface TaskRegistry {

    void registerOnce(String name, Task task, ProvidedProducts providedProducts);
    void register(String name, Task task, ProvidedProducts providedProducts);

}
