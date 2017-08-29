Getting Started
===============

Install Loom
------------

Change to your project directory (or create a new directory) and enter:

::

    curl -s https://loom.builders/installer.sh | sh

For more details, read the `Installing and Updating`_ section.

Adjust build configuration
--------------------------

Edit the ``module.yml`` file according to your needs. Some quick examples:

This is a very basic project configuration without any dependencies, targeting Java 8 (default):

.. code-block:: yaml

    settings:
      moduleName: builders.loom


This is a more advanced configuration with a few dependencies (compile & test), targeting Java 8,
checking code with Checkstyle_, SpotBugs_ and running Tests with Junit4_:

.. code-block:: yaml
   :caption: module.yml

    settings:
      moduleName: builders.loom
      javaPlatformVersion: 8
    plugins:
      - checkstyle
      - spotbugs
      - junit4
    compileDependencies:
      - com.google.guava:guava:21.0
    testDependencies:
      - junit:junit:4.12


For more details, read the `Configuration`_ section.


Run build
---------

::

    ./loom build


.. _Installing and Updating: installing-and-updating.html
.. _Configuration: configuration.html
.. _Checkstyle: http://checkstyle.sourceforge.net
.. _SpotBugs: https://spotbugs.github.io
.. _Junit4: http://junit.org/junit4/
