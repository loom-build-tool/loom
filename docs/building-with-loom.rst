Building with Loom
==================

Loom has a very simple command line interface. The usage is::

    ./loom [option...] [product|goal...]

Just enter ``./loom -h`` so see the full options reference.


Plugins, tasks products and goals
---------------------------------

``./loom -p`` prints all products and goals available with your current configuration.

To understand what products and goals are, you should get the full picture:

product
    A product is the smallest unit in the Loom build chain. It might be a file
    (e.g. a .jar file), a directory (e.g. compiled classes) or something else.

goal
    A goal is a list of dependent products and goals.
    It does't add any functionality / products by itself.

task
    Every task in Loom is responsible for providing (building/producing) a product.

plugin
    A plugin registers one or more tasks to the Loom build chain. A plugin
    also defines the dependency between products and goals.


You may also want to use ``loom -p dot`` to generate a ``.dot`` file, containing
all the dependencies between the products and goals. This file can be turned into
a graphical representation (e.g. ``.png``) by using `Graphviz <http://www.graphviz.org>`_.


Examples
--------

Default build
~~~~~~~~~~~~~

``./loom build`` requests the ``build`` goal.


Build just some products
~~~~~~~~~~~~~~~~~~~~~~~~

``./loom jar javadocJar`` requests the products ``jar`` and ``javadocJar``.


Clean before
~~~~~~~~~~~~

``./loom -c build`` requests the ``build`` goal after removing the directories ``.loom`` and ``build``.


Build without caches
~~~~~~~~~~~~~~~~~~~~

``./loom -n build`` requests the ``build`` goal without writing any caches (or reading existing ones).
