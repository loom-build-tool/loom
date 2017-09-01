Plugin maven
============

This plugin allows Loom to access the repository offered by `Apache Maven`_.


Products
--------

The only user visible product of this Plugin is ``mavenArtifact``. It installs
all Jar products of your project into the local maven repository, which is located
at ``~/.m2/repository/`` by default.
This allows sharing artifacts by Loom for other build tools that can access
the local Maven repository.


Settings
--------

repositoryUrl
    The URL of the maven repository to use when downloading artifacts.
    By default this is: ``https://repo.maven.apache.org/maven2/``.

groupAndArtifact
    The *groupId* and *artifactId* used by the ``mavenArtifact`` product when
    creating a ``pom.xml`` and installing to your local repository.
    The format is: ``yourGroupId:yourArtifactId``.
    The also required *version* is specified through the
    ``--artifact-version`` or ``-a`` parameters of the Loom CLI.


.. _Apache Maven: https://maven.apache.org
