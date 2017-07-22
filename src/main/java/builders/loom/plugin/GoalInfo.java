package builders.loom.plugin;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class GoalInfo {

    private final String name;
    private final Set<String> usedProducts;

    public GoalInfo(final String name, final Set<String> usedProducts) {
        this.name = name;
        this.usedProducts = Collections.unmodifiableSet(usedProducts);
    }

    public String getName() {
        return name;
    }

    public Set<String> getUsedProducts() {
        return usedProducts;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GoalInfo goalInfo = (GoalInfo) o;
        return Objects.equals(name, goalInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
