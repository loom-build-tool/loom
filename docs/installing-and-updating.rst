Installing and Updating
=======================

Automated installation
----------------------

To install the most recent version of Loom, change to your project directory and enter::

    curl -s https://loom.builders/installer.sh | sh

*The same command is also used to update an existing installation.*

If no errors were reported, Loom is installed to your system / project.

If you want to install a specific (older) version of loom instead (e.g. 1.0.0), enter::

    curl -s https://loom.builders/installer.sh | sh -s 1.0.0


Source Control Management
-------------------------

If you're using a SCM system like Git, you probably want to add the *Loom Launcher*
and the *Loom Installer* to your repository to ensure reproducible builds for your project::

    git add loom loom.cmd loom-installer


Furthermore, you probably want to exclude some directories from being committed.
Example ``.gitignore`` file::

    .loom
    /build/

    # In case .jar files are globally ignored
    !loom-installer.jar


Behind the scenes
-----------------

To guarantee reproducible builds while maintaining the size of files added to your project at
a minimum, the Loom installation is split into two parts:
a *project wide* one (the *Loom Installer* and the *Loom Launcher*) and
a *system wide* one (the *Loom Library*).

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

**In your system:**

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
