package builders.loom.api;

import java.util.Map;
import java.util.Set;

public interface ModuleGraphAware {
    void setTransitiveModuleGraph(Map<Module, Set<Module>> moduleGraph);
}
