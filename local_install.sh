#!/bin/sh
VERSION=1.0.0
TARGET=~/.loom/binary/$VERSION/
./gradlew clean zip &&
    rm -rf $TARGET &&
    unzip build/distributions/loom-$VERSION.zip -d $TARGET
