package jobt.api;

import java.util.Map;

public interface ExecutionContext {

    Map<String, ProductPromise> getProducts();

}
