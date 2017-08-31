Getting Started
===============

Install Loom
------------

Change to your project directory and enter::

    curl -s https://loom.builders/installer.sh | sh

For more details, read the `Installing and Updating`_ section.


Adjust build configuration
--------------------------

Very basic projects (without any external dependencies) don't need a configuration at all.

A typical configuration with a few external dependencies might look like this:

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - junit4
    compileDependencies:
      - com.google.guava:guava:21.0
    testDependencies:
      - junit:junit:4.12


For more details, read the `Configuration`_ section.


Run build
---------

To start a build using Loom, enter::

    ./loom build

For more details, read the `Building with Loom`_ section.


More examples
-------------

If you want to see more examples on how projects are built using Loom, visit our
`GitHub loom-examples project <https://github.com/loom-build-tool/loom-examples>`_.


.. _Installing and Updating: installing-and-updating.html
.. _Configuration: configuration.html
.. _Building with Loom: building-with-loom.html
.. _Checkstyle: http://checkstyle.sourceforge.net
.. _SpotBugs: https://spotbugs.github.io
.. _Junit4: http://junit.org/junit4/
