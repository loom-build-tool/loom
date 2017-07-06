#!/bin/sh
set -e

DEFAULT_VERSION=1.0.0

loom_version=${1:-$DEFAULT_VERSION}
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

test -d loom-downloader || mkdir loom-downloader

echo "Fetch Loom Downloader $loom_version from $downloader_url ..."
echo "distributionUrl=$lib_url" > loom-downloader/loom-downloader.properties
curl -f -s -S "$downloader_url" > loom-downloader/loom-downloader.jar

if [ ! -d $loom_base ]; then
    java -jar loom-downloader/loom-downloader.jar
fi

echo "Create loom build scripts"
cp "$loom_base/scripts/loom" .
chmod 755 loom

cp "$loom_base/scripts/loom.cmd" .

if [ ! -e build.yml ]; then
    echo "Create initial build.yml"
    cp "$loom_base/scripts/build.yml" .
fi

echo "Done. Adjust \`build.yml\` to your needs and then run \`./loom build\` to start your build."
