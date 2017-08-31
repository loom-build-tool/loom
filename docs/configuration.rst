Configuration
=============

The build configuration for projects built with Loom is located in a file called ``module.yml``.
For multi-module builds, every module may have such a file.


Conventions
-----------

For a better understanding of how the configuration works and what can be done, it is important
to first understand the conventions of Loom.

Directory layout
~~~~~~~~~~~~~~~~

Loom knows two kinds of directory layouts:

Single module
    Single module projects have
    their sources in ``src/main/java/``,
    their resources in ``src/main/resources/``,
    their test sources in ``src/test/java/`` and
    their test resources in ``src/test/resources/``.

Multi module
    Multi module projects have a directory ``modules/`` and a subdirectory per module
    beneath that ``modules/`` directory. In the module subdirectories, the structure is as with
    single modules (starting with a ``src/`` directory).


These two directory layouts are mutually exclusive and cannot be used together.

Every module might have a configuration, called ``module.yml``. For single modules, this file
is located in the root directory of your project. For multi modules, this file is located
in each module subdirectory beneath the ``modules/`` directory.


Module configuration
--------------------

A ``module.yml`` configuration has up to five sections.

plugins
    A list of plugins to activate for the module.
    Some plugins in Loom are active by default: ``eclipse``, ``idea``, ``java`` and ``maven``.

settings
    A list of global or plugin specific settings. Plugin specific settings starts with
    the plugin name followed by a dot as a prefix (e.g. ``java.mainClassName`` for the
    setting ``mainClassName`` of the ``java`` plugin). Global settings have no prefix.

moduleCompileDependencies
    A list of modules (of the same project) this module depends on.
    This is only available within multi module projects.

compileDependencies
    A list of compile time dependencies.

testDependencies
    A list of dependencies, required when running tests.


Example:

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - checkstyle
    settings:
      moduleName: builders.loom.example.app
      javaPlatformVersion: 8
      java.mainClassName: builders.loom.example.app.Main
      checkstyle.configLocation: config/checkstyle/checkstyle.xml
    moduleCompileDependencies:
      - builders.loom.example.api
    compileDependencies:
      - com.google.guava:guava:21.0
    testDependencies:
      - junit:junit:4.12



Global settings
~~~~~~~~~~~~~~~

moduleName
    By default, the module name of a module in a multi module project is the name of
    the module subdirectory. In a single module setup, the default name is *unnamed*.
    The recommended way to define/overwrite this, is to create a ``module-info.java`` file
    in each module. If you do not want to create this file, you can specify the name
    of the module using this setting.

javaPlatformVersion
    By default, Loom compiles your sources with Java 9 as the target Java version. If you want
    to lower the required Java version of your application, you can specify 6, 7 or 8 and
    Loom will use JDKs cross-compile functionality to support older Java releases.
    Please note, that Loom itself always requires Java 9 to run.
