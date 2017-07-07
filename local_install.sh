#!/bin/sh
VERSION=1.0.0
TARGET=~/.loom/binary/
test -d $TARGET || mkdir -p $TARGET
./gradlew clean zip &&
    rm -rf $TARGET/loom-$VERSION &&
    unzip build/distributions/loom-$VERSION.zip -d $TARGET
