Plugin pmd
==========

The PMD plugin provides the functionality of PMD_ to Loom.
See the PMD website for a comprehensive documentation of existing RuleSets and how to create
custom ones.

Loom uses PMD version 5.8.1.


Products
--------

This plugin configures two products: ``pmdMainReport`` and ``pmdTestReport`` to
provide reports for sources in ``src/main/java/`` and ``src/test/java/`` respectively.

These products are registered for the ``check`` goal (which is part of the ``build`` goal).


Settings
--------

minimumPriority
    Rules with a lower priority will not be used. Possible priorities are:
    ``LOW`` (default), ``MEDIUM_LOW``, ``MEDIUM``, ``MEDIUM_HIGH`` and ``HIGH``

ruleSets
    A comma separated list of RuleSets.
    By default, only ``rulesets/java/basic.xml`` is configured.
    See the `PMD RuleSet index`_ and `PMD RuleSet files`_ for a full reference of all available
    RuleSets.

.. _PMD: https://pmd.github.io
.. _PMD RuleSet index: https://pmd.github.io/pmd-5.8.1/pmd-java/rules/index.html
.. _PMD RuleSet files: https://raw.githubusercontent.com/pmd/pmd/pmd_releases/5.8.1/pmd-java/src/main/resources/rulesets/java/rulesets.properties
