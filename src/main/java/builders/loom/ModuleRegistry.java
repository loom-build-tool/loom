package builders.loom;

import java.util.ArrayList;
import java.util.List;

public class ModuleRegistry {

    private final List<Module> modules = new ArrayList<>();

    public void register(final Module module) {
        modules.add(module);
    }

}
