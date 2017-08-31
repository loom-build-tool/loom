Continuous Integration
======================

Travis CI
---------

To build (and test) a project built with Loom using `Travis CI <http://travis-ci.org>`_,
add this to your ``.travis.yml`` file::

    language: java
    jdk:
      - oraclejdk9
    script: ./loom -n build
    cache:
      directories:
        - $HOME/.loom/library/
        - $HOME/.loom/repository/

For more information about Travis CI visit their `online documentation <https://docs.travis-ci.com>`_.
