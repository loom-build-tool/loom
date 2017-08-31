# Loom Build Tool

Loom is a modern build tool for Java based projects.
It focuses on simplicity, conventions, ease of use and performance.
It uses a very simple, declarative configuration (YAML) and
convention over configuration to reduce the configuration to a bare minimum.


# Requirements

Loom requires **JDK 9** to build and run but it can build projects
**targeting Java 6-9** through JDKs cross-compile functionality.

Java 9 can be downloaded from [http://jdk.java.net/9/](http://jdk.java.net/9/).
Loom is excessively tested against build `jdk-9+181`.


# Use Loom in your project

Change to your project directory and enter:

    curl -s https://loom.builders/installer.sh | sh

After installation succeeded, run your first build by entering:

    ./loom build

This builds sources beneath `src/main/java/` without external dependencies.


For more info, visit the
[Loom website](https://loom.builders) and the
[Loom documentation](https://loom-build-tool.readthedocs.io).
