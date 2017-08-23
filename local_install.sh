#!/bin/sh
VERSION=1.0.0-rolling
TARGET=~/.loom/library/
test -d $TARGET || mkdir -p $TARGET
./gradlew clean zip &&
    rm -rf $TARGET/loom-$VERSION &&
    unzip build/distributions/loom-$VERSION.zip -d $TARGET
