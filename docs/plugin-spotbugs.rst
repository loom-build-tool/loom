Plugin spotbugs
===============

The SpotBugs plugin provides the functionality of SpotBugs_ to Loom.

From the SpotBugs website: *SpotBugs is the spiritual successor of FindBugs,
carrying on from the point where it left off with support of its community.*

Loom uses SpotBugs version 3.1.0-RC5, FbContrib version 7.0.3.sb, FindSecBugs version 1.7.1.


Products
--------

This plugin configures two products: ``spotbugsMainReport`` and ``spotbugsTestReport`` to
provide reports for compiled main and test classes respectively.

These products are registered for the ``check`` goal (which is part of the ``build`` goal).


Settings
--------

priorityThreshold
    Defines the minimum priority a bug has to have to get reported.
    Possible levels are: ``LOW``, ``NORMAL`` (default), ``HIGH``.

customPlugins
    Additional SpotBugs plugins can be activated by listing them comma separated.
    Currently available plugins are:
    ``FbContrib`` (see `FbContrib website <http://fb-contrib.sourceforge.net/>`_) and
    ``FindSecBugs`` (see `FindSecBugs websie <http://find-sec-bugs.github.io/>`_).


.. _SpotBugs: https://spotbugs.github.io
.. _FindBugs: http://findbugs.sourceforge.net