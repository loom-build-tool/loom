Plugin java
===========



Resource filtering
------------------

The java plugin comes with resource filtering functionality. Filtering means, replacing special
variable placeholders (``${placeholder}``) with dynamic values.

When resolving placeholders, Loom will first try to find a *system property*
(specified via the ``-D`` switch on the CLI) with the name of the placeholder.
If no system property with that name could be found, Loom will try to find
an *environment variable* with that name.

The Loom build will fail, if a placeholder can't be resolved. If you want to have
default values instead, use the syntax ``${placeholder:default-value}``.

The following placeholders are registered automatically:

* ``${project.version}`` will be replaced by the version defined for the build.
  Don't forget to specify a version for the build via the ``--artifact-version`` or ``-a`` parameter.

See the setting ``resourceFilterGlob`` for how to control which files should be filtered.


Settings
--------

mainClassName
    This defines the fully qualified name of a Java class that contains a main method
    that should be used automatically, when launching the jar file this class is contained in.
    Technically, this sets the ``Main-Class`` attribute of the ``META-INF/MANIFEST.MF`` file
    of the jar file. For Java 9 modularized jar files, this also sets the ``ModuleMainClass``
    attribute to the corresponding ``module-info.class`` file.

resourceFilterGlob
    Controls, which resource files should be filtered. A value of ``*.properties`` for example,
    ensures, that all files with a suffix of ``.properties`` within ``src/main/resources/``
    and ``src/test/resources/`` of your module will be filtered.
    By default, no files will be filtered.
