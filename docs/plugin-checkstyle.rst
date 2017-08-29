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

The default configuration uses a set of very basic checks defined as Loom default. Please note, that this default (built-in) check configuration may be updated in future versions of Loom.


More advanced example
---------------------

.. code-block:: yaml
   :caption: module.yml

    plugins:
      - checkstyle
    settings:
      checkstyle.configLocation: config/checkstyle/checkstyle.xml


In this example, a custom configuration ``config/checkstyle/checkstyle.xml`` will be used.
This directory/file has to be created within your project directory.

Other built-in check are ``/google_checks.xml`` and ``/sun_checks.xml``
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
