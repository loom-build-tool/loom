package jobt.plugin;

public interface Plugin {

    String getRegisteredPhase();
    void run() throws Exception;

}
