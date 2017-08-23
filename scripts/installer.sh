#!/usr/bin/env sh

set -e

default_loom_version=@LOOM_VERSION@

loom_version=${1:-$default_loom_version}
installer_url="https://loom.builders/loom-installer-$loom_version.jar"
lib_url="https://loom.builders/loom-$loom_version.zip"

# Detect environment for special handling
cygwin=false
case "$(uname -s)" in
    CYGWIN*)
        cygwin=true
        ;;
esac

# Define location for Loom Library
if [ -z "$LOOM_USER_HOME" ]; then
    if $cygwin; then
        LOOM_USER_HOME="$LOCALAPPDATA/Loom/Loom"
    else
        LOOM_USER_HOME=~/.loom
    fi
fi

# Find the java executable
if [ -n "$JAVA_HOME" ] ; then
    javacmd="$JAVA_HOME/bin/java"
    if [ ! -x "$javacmd" ] ; then
        echo "ERROR: Can't execute $javacmd" >&2
        echo "Please ensure JAVA_HOME is configured correctly: $JAVA_HOME" >&2
        exit 1
    fi
else
    if ! which java >/dev/null 2>&1 ; then
        echo "ERROR: Can't find Java - JAVA_HOME is not set and no java was found in your PATH" >&2
        exit 1
    fi

    javacmd="$(which java)"
    echo "Warning: JAVA_HOME environment variable is not set - using $javacmd from path" >&2
fi

# Adjust paths for Cygwin
if $cygwin; then
    javacmd=$(cygpath --unix "$javacmd")
fi

echo "Installing Loom to $(pwd)"

test -d loom-installer || mkdir loom-installer

# Download Loom Installer
echo "Fetch Loom Installer $loom_version from $installer_url ..."
echo "distributionUrl=$lib_url" > loom-installer/loom-installer.properties

if which curl >/dev/null 2>&1 ; then
    curl -f -s -S -o loom-installer/loom-installer.jar "$installer_url"
elif which wget >/dev/null 2>&1 ; then
    wget -nv -O loom-installer/loom-installer.jar "$installer_url"
else
    echo "Neither curl nor wget found to download $installer_url" >&2
    exit 1
fi

# Launch Loom Installer
loom_versioned_base="$LOOM_USER_HOME/library/loom-$loom_version"

"$javacmd" -jar loom-installer/loom-installer.jar .

echo "Done. Run \`./loom build\` to start your build."
