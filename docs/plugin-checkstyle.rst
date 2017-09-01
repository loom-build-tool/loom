Plugin checkstyle
=================

The Checkstyle plugin provides the functionality of Checkstyle_ to Loom.
See the Checkstyle website for a comprehensive documentation of how to write custom configurations.

Loom uses Checkstyle version 8.2.


Products
--------

This plugin configures two products: ``checkstyleMainReport`` and ``checkstyleTestReport`` to
provide reports for sources in ``src/main/java/`` and ``src/test/java/`` respectively.

These products are registered for the ``check`` goal (which is part of the ``build`` goal).


Minimum example
---------------

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - checkstyle

By default, the checkstyle plugin reads its configuration from ``config/checkstyle/checkstyle.xml``.
This directory/file has to be located in the project directory (or in each of your module
directories for multi module projects). This default can be changed via the setting ``configLocation``.


More advanced example
---------------------

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - checkstyle
    settings:
      checkstyle.configLocation: ../config/checkstyle/checkstyle.xml


In this example, a custom location ``../config/checkstyle/checkstyle.xml`` will be used.
This is handy if you want to have one checkstyle configurations for multiple modules.

Checkstyle itself comes with two built-in checks: ``/google_checks.xml`` and ``/sun_checks.xml``
(note the leading ``/`` for built-in check configurations).
Both are part of the official Checkstyle package and are maintained by the `Checkstyle Team`_.

Settings
--------

configLocation
    Path to a file containing the Checkstyle configuration. By default,
    ``config/checkstyle/checkstyle.xml`` is used. The path is relative to the
    Loom module directory, so each module requires its own configuration.
    If you want a checkstyle configuration per project, you can set
    ``../config/checkstyle/checkstyle.xml`` for example.


Notes
-----

* This plugin handles the cache configuration of Checkstyle automatically.
  Do **not** include a ``cacheFile`` property within your ``checkstyle.xml`` -- otherwise
  Loom's CLI options to disable caching (``--no-cache`` or ``-n``) wouldn't work as expected.


.. _Checkstyle: http://checkstyle.sourceforge.net
.. _Checkstyle Team: http://checkstyle.sourceforge.net/team-list.html
