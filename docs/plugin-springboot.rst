Plugin springboot
=================

To build a Spring Boot application, first create a ``module.yml`` configuration:

.. code-block:: yaml

    plugins:
      - springboot
    settings:
      springboot.version: 1.5.4.RELEASE
    compileDependencies:
      - org.springframework.boot:spring-boot-starter-web:1.5.4.RELEASE


Alternative 1 - Build and run Fat-JAR
-------------------------------------

The first, any simplest way to build/launch a Spring Boot application is the Fat-JAR approach,
where a single jar containing all other dependencies is created and launched.

::

    ./loom build springBootFatJarApplication
    java -jar build/unnamed/springboot-fatjar/unnamed-fatjar.jar


Alternative 2 - Build application for Docker container
------------------------------------------------------

The second approach is, to create a Docker image for your application and start a Docker container
with that image.

First, create a file called ``Dockerfile``:

.. code-block:: docker
   :caption: Dockerfile

    FROM openjdk:9-slim

    EXPOSE 8080

    WORKDIR /app

    COPY build/unnamed/springboot/META-INF /app/META-INF
    COPY build/unnamed/springboot/org /app/org
    COPY build/unnamed/springboot/BOOT-INF/lib /app/BOOT-INF/lib
    COPY build/unnamed/springboot/BOOT-INF/classes /app/BOOT-INF/classes

    CMD ["java", "-cp", "/app", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.JarLauncher"]


Then, build the application, the docker image and launch a container::

    ./loom build
    docker build -t my-spring-boot-app .
    docker run -ti my-spring-boot-app


Settings
--------

version
    Use this setting to specify the version of the ``Spring Boot Loader`` to use.
    Spring Boot 1.5.x releases are known to work.
