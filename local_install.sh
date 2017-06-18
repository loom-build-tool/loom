#!/bin/sh
VERSION=1.0.0
TARGET=~/.jobt/binary/$VERSION/
./gradlew clean zip &&
    rm -rf $TARGET &&
    unzip build/distributions/jobt-$VERSION.zip -d $TARGET
