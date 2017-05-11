# Java Optimized Build Tool (jobt)

What is jobt?

- A modern build tool for Java projects
- Very fast build execution
- Very small footprint
- Configured via YAML build script
- Dependency management (Maven repository)

What is jobt **not**?

- A feature rich and versatile system like [Gradle](https://gradle.org) or [Maven](http://maven.apache.org)

# Prerequisites

- JDK 9



# Example build.yml

```yaml
project:
  groupId: com.github
  artifactId: example-project
  version: 1.0.0-SNAPSHOT
```

This builds a project without any dependencies targeting Java 9.


# More comprehensive build.yml
```yaml
project:
  groupId: com.github
  artifactId: example-project
  version: 1.0.0-SNAPSHOT
plugins:
  - checkstyle
configuration:
  javaPlatformVersion: 8
dependencies:
  - com.google.guava:guava:21.0
testDependencies:
  - junit:junit:4.12
```

This builds a project with a few dependencies (compile & test), checking sources with Checktsyle, targeting Java 8.


# Install jobt to your project

```sh
cd /path-to-your-project
curl -s https://s3.eu-central-1.amazonaws.com/jobt/installer.sh | sh
vi build.yml # adjust the build.yml to your needs
./jobt build
```
