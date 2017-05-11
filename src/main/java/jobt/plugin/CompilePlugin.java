package jobt.plugin;

public interface CompilePlugin extends Plugin {

    Boolean compile(String classPath) throws Exception;

}
