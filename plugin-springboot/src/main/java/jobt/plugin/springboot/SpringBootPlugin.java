package jobt.plugin.springboot;

import jobt.api.AbstractPlugin;

public class SpringBootPlugin extends AbstractPlugin<SpringBootPluginSettings> {

    public SpringBootPlugin() {
        super(new SpringBootPluginSettings());
    }

    @Override
    public void configure() {
        final SpringBootPluginSettings pluginSettings = getPluginSettings();

        if (pluginSettings.getVersion() == null) {
            throw new IllegalStateException("Missing required setting: springboot.version");
        }

        task("springBootApplication")
            .impl(() -> new SpringBootTask(getBuildConfig(), pluginSettings))
            .provides("springBootApplication")
            .uses("processedResources", "compilation", "compileDependencies",
                "pluginDependencies.springBootApplication")
            .deps("org.springframework.boot:spring-boot-loader:" + pluginSettings.getVersion())
            .register();

        goal("build")
            .requires("springBootApplication")
            .register();
    }

}
