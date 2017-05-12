#!/bin/sh
set -e

jobt_version=1.0.0
jobt_base=~/.jobt/binary/$jobt_version
download_url="https://s3.eu-central-1.amazonaws.com/jobt/jobt-downloader-$jobt_version.jar"

if [ ! -d jobt-downloader ]; then
    mkdir jobt-downloader
    curl -s "$download_url" > jobt-downloader/jobt-downloader.jar
fi

if [ ! -d $jobt_base ]; then
    java -cp jobt-downloader/jobt-downloader.jar jobt.JobtDownloader $jobt_version
fi

if [ ! -f jobt ]; then
	echo "Create jobt build script"
    cp "$jobt_base/scripts/jobt" .
    chmod 755 jobt
fi

if [ ! -d build.yml ]; then
    echo "Create build.yml"
    cp "$jobt_base/scripts/build.yml" .
fi

echo "Done. Run ``./jobt`` to start your build."
