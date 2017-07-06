#!/bin/sh
set -e

loom_version=1.0.0
downloader_url="https://loom.builders/loom-downloader-$loom_version.jar"
lib_url="https://loom.builders/loom-$loom_version.zip"

case "$(uname -s)" in
    CYGWIN*)
        loom_base=$LOCALAPPDATA/Loom/binary/loom-$loom_version
        ;;
    *)
        loom_base=~/.loom/binary/loom-$loom_version
        ;;
esac

if [ ! -d loom-downloader ]; then
    mkdir loom-downloader
    echo "distributionUrl=$lib_url" > loom-downloader/loom-downloader.properties
    curl -s "$downloader_url" > loom-downloader/loom-downloader.jar
fi

if [ ! -d $loom_base ]; then
    java -jar loom-downloader/loom-downloader.jar
fi

if [ ! -e loom ]; then
    echo "Create loom build script"
    cp "$loom_base/scripts/loom" .
    chmod 755 loom
fi

if [ ! -e loom.cmd ]; then
    echo "Create loom.cmd build script"
    cp "$loom_base/scripts/loom.cmd" .
fi

if [ ! -e build.yml ]; then
    echo "Create build.yml"
    cp "$loom_base/scripts/build.yml" .
fi

echo "Done. Run ``./loom build`` to start your build."
