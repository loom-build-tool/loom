Plugin junit
============

This plugin allows the use of `JUnit`_ within projects build with Loom.

Loom uses the JUnit Platform version 1.0.2 (which is part of JUnit 5).


Products
--------

This plugin configures the product ``junitReport``.

The product ``junitReport`` is registered for the ``check`` goal (which is part of the ``build`` goal).


Configuration
-------------

To execute Tests with JUnit 5 natively, this is your configuration:

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - junit
    testDependencies:
      - org.junit.jupiter:junit-jupiter-engine:5.0.2


Legacy configuration
--------------------

If you have a project which currently depends on the API of JUnit 4 and don't want to update
your code to the new API of JUnit 5, you can configure Loom to use the *JUnit Vintage*
dependency to test your project without any changes.

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - junit
    testDependencies:
      - org.junit.vintage:junit-vintage-engine:4.12.2


Settings
--------

This plugin has no settings.


.. _JUnit: http://junit.org
