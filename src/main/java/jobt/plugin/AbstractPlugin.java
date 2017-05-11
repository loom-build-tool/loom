package jobt.plugin;

public abstract class AbstractPlugin implements Plugin {

    private final String registeredPhase;

    public AbstractPlugin(final String registeredPhase) {
        this.registeredPhase = registeredPhase;
    }

    @Override
    public String getRegisteredPhase() {
        return registeredPhase;
    }

}
