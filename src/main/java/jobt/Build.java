package jobt;

import java.util.ArrayList;
import java.util.List;

import jobt.plugin.CompilePlugin;
import jobt.plugin.Plugin;

public class Build {

    private final List<Plugin> plugins = new ArrayList<>();

    public void registerPlugin(final Plugin plugin) {
        plugins.add(plugin);
    }

    public void compile(final String classPath) throws Exception {
        System.out.println("Start compiling");

        for (final Plugin plugin : plugins) {
            if (plugin instanceof CompilePlugin) {
                ((CompilePlugin) plugin).compile(classPath);
            }
        }
    }

    public void test(final String testClassPath) {
        System.out.println("Test not implemented yet");
    }

    public void assemble() {
        System.out.println("Assemble not implemented yet");
    }

}
