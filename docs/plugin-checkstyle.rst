Plugin checkstyle
=================

The Checkstyle plugin provides the functionality of Checkstyle_ to Loom.
See the Checkstyle website for a comprehensive documentation of how to write custom configurations.

Loom uses Checkstyle version 8.1.


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


.. _Checkstyle: http://checkstyle.sourceforge.net
.. _Checkstyle Team: http://checkstyle.sourceforge.net/team-list.html
