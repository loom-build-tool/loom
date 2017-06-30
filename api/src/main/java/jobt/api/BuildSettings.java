package jobt.api;

import java.io.Serializable;

public interface BuildSettings extends Serializable {

    JavaVersion getJavaPlatformVersion();

}
