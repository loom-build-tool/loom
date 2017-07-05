package builders.loom.plugin.springboot;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestBuilder {

    private final Attributes mainAttributes;

    public ManifestBuilder(final Manifest manifest) {
        mainAttributes = manifest.getMainAttributes();
    }

    public ManifestBuilder put(final Attributes.Name attr, final String value) {
        mainAttributes.put(attr, value);
        return this;
    }

    public ManifestBuilder put(final String attr, final String value) {
        mainAttributes.put(new Attributes.Name(attr), value);
        return this;
    }

}
