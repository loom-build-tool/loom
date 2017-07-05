#!/bin/sh
set -e

loom_version=1.0.0
loom_base=~/.loom/binary/$loom_version
download_url="https://loom.builders/loom-downloader-$loom_version.jar"

if [ ! -d loom-downloader ]; then
    mkdir loom-downloader
    curl -s "$download_url" > loom-downloader/loom-downloader.jar
fi

if [ ! -d $loom_base ]; then
    java -cp loom-downloader/loom-downloader.jar builders.loom.LoomDownloader $loom_version
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
