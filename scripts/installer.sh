#!/usr/bin/env sh

set -e

default_loom_version=1.0.0

loom_version=${1:-$default_loom_version}
downloader_url="https://loom.builders/loom-downloader-$loom_version.jar"
lib_url="https://loom.builders/loom-$loom_version.zip"

# Detect environment for special handling
cygwin=false
case "$(uname -s)" in
    CYGWIN*)
        cygwin=true
        ;;
esac

# Define location for Loom library
if $cygwin; then
    loom_base_dir="$LOCALAPPDATA/Loom"
else
    loom_base_dir=~/.loom
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

test -d loom-downloader || mkdir loom-downloader

# Download Loom Downloader
echo "Fetch Loom Downloader $loom_version from $downloader_url ..."
echo "distributionUrl=$lib_url" > loom-downloader/loom-downloader.properties

if which curl >/dev/null 2>&1 ; then
    curl -f -s -S -o loom-downloader/loom-downloader.jar "$downloader_url"
elif which wget >/dev/null 2>&1 ; then
    wget -nv -O loom-downloader/loom-downloader.jar "$downloader_url"
else
    echo "Neither curl nor wget found to download $downloader_url" >&2
    exit 1
fi

# Launch Loom Downloader
loom_versioned_base="$loom_base_dir/binary/loom-$loom_version"

if [ ! -d $loom_versioned_base ]; then
    exec "$javacmd" -jar loom-downloader/loom-downloader.jar
fi

# Create scripts
echo "Create loom build scripts"
cp "$loom_versioned_base/scripts/loom" .
chmod 755 loom

cp "$loom_versioned_base/scripts/loom.cmd" .

# Create build.yml template
if [ ! -e build.yml ]; then
    echo "Create initial build.yml"
    cp "$loom_versioned_base/scripts/build.yml" .
fi

echo "Done. Adjust \`build.yml\` to your needs and then run \`./loom build\` to start your build."
