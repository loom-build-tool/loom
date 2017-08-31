Installing and Updating
=======================


Automated installation
----------------------

To install the most recent version of Loom to your project, change to your project directory and enter:

::

    curl -s https://loom.builders/installer.sh | sh

To install a specific version (e.g. 1.0.0), enter:

::

    curl -s https://loom.builders/installer.sh | sh -s 1.0.0

These commands are also used to update an existing project to a different Loom version.

**That's it!**

If no errors were reported, Loom is installed to your system / project.
Edit the build configuration ``module.yml`` according to your needs (see `Configuration`_) and run the build
with ``./loom build`` (on Unix) or ``loom build`` (on Windows).

Read on if you want some more detailed information.


Behind the scenes
-----------------

After a successful ``installer.sh`` run, the following files were created:

**In your project directory:**

``loom-installer/loom-installer.jar``
    The *Loom Installer* -- a tiny program that installs Loom on systems where it is missing.
    It is executed by the *Loom Launcher* if necessary.

``loom-installer/loom-installer.properties``
    Configuration of the *Loom Installer* -- e.g. the download URL of Loom and its version.

``loom``
    The *Loom Launcher* for Unix

``loom.cmd``
    The *Loom Launcher* for Windows

You can/should check in all these directories/files to your Source Code Management (SCM) system.
This ensures, that your project will be built with the exact same version of Loom at any time, event
if another developer is working on that project.

Loom itself will create the directories ``.loom`` and ``build`` on first launch.
These two directories should **not** be checked in to your SCM system.

Example ``.gitignore`` file::

    .loom
    /build/

    # In case .jar files are globally ignored
    !loom-installer.jar


**In your user home**

The *Loom Library* is installed to a user specific directory on your machine, that is dependent on
your operating system:

* On Unix: ``~/.loom/``
* On Windows: ``%LOCALAPPDATA%\Loom\Loom\`` (where ``%LOCALAPPDATA%`` is ``C:\Users\{username}\AppData\Local`` by default)

You can override these operating system specific defaults by setting the ``LOOM_USER_HOME``
environment variable to a directory of your choice.

Manual installation
-------------------

If you want to install Loom manually (**discouraged**), follow these steps:

* Change to your project directory
* Create a directory named ``loom-installer``
* Create a file named ``loom-installer/loom-installer.properties``, which points to
  the *Loom Library* URL (example: ``distributionUrl=https://loom.builders/loom-1.0.0.zip``)
* Download the *Loom Installer* to a file named ``loom-installer/loom-installer.jar``.
  You can download it from ``https://loom.builders/loom-installer-1.0.0.jar``.
* Launch the *Loom Installer* by entering ``java -jar loom-installer/loom-installer.jar .``


.. _Configuration: configuration.html
