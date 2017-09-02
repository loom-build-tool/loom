Plugin eclipse
==============

This plugin creates all necessary files for `Eclipse IDE for Java Developers`_ to work on your project.


Products
--------

This plugin configures two products:

eclipse
    Creates and maintains the files ``.project``, ``.classpath`` and ``.settings/org.eclipse.jdt.core.prefs``.
    One set of project files will be created per module.

    **Note**: Project files (``.project`` and ``**/*.prefs``) will be patched to meet the minimum requirements of an *Eclipse IDE* project.
    Existing configuration (*natures*/*buildSpecs*) will not be changed by the task.

cleanEclipse
    Removes the files created/patched by *eclipse* product: ``.project``, ``.classpath``, ``.settings/*``


Settings
--------

This plugin has no settings.


.. _Eclipse IDE for Java Developers: https://www.eclipse.org/downloads/packages/
