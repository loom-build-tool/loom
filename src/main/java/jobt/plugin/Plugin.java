package jobt.plugin;

public interface Plugin {

    TaskStatus run(String task) throws Exception;

}
