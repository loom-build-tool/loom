Plugin spotbugs
===============

The SpotBugs plugin provides the functionality of SpotBugs_ to Loom.

From the SpotBugs website: *SpotBugs is the spiritual successor of FindBugs_,
carrying on from the point where it left off with support of its community.*

Loom uses SpotBugs version 3.1.0, FbContrib version 7.0.5.sb, FindSecBugs version 1.7.1.


Products
--------

This plugin configures two products: ``spotbugsMainReport`` and ``spotbugsTestReport`` to
provide reports for compiled main and test classes respectively.

These products are registered for the ``check`` goal (which is part of the ``build`` goal).


Settings
--------

customPlugins
    Additional SpotBugs plugins can be activated by listing them comma separated.
    Currently available plugins are:
    ``FbContrib`` (see `FbContrib website <http://fb-contrib.sourceforge.net/>`_) and
    ``FindSecBugs`` (see `FindSecBugs website <http://find-sec-bugs.github.io/>`_).

effort
    The effort level SpotBugs should use.
    Possible levels are: ``min``, ``default`` (default), ``max``.
    Higher values may find more bugs, but also have more cpu and memory consumption
    (and thus may take longer).

excludeFilterFiles
    Filename of a filter configuration (blacklist).
    Example: ``config/spotbugs/exclude-filters.xml``.
    See `SpotBugs Filter file`_ for details on how to create such a filter.

excludeTests
    Boolean flag to allow the exclusion of test classes. As it is *not* recommended,
    the default is ``false``. Set it to ``true`` to have standard Maven behaviour.

includeFilterFiles
    Filename of a filter configuration (whitelist).
    Example: ``config/spotbugs/include-filters.xml``.
    See `SpotBugs Filter file`_ for details on how to create such a filter.

reporter
    The report output format.
    Possible values are: ``html`` (default) and ``xml``.

reportLevel
    Defines the minimum priority a bug has to have to get reported.
    Possible levels are: ``LOW``, ``NORMAL`` (default), ``HIGH``.


.. _SpotBugs: https://spotbugs.github.io
.. _FindBugs: http://findbugs.sourceforge.net
.. _SpotBugs Filter file: http://spotbugs.readthedocs.io/en/latest/filter.html
