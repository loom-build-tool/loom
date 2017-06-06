#!/bin/sh
VERSION=1.0.0
TARGET=~/.jobt/binary/$VERSION/
test -e $TARGET || mkdir -p $TARGET
./gradlew clean zip && unzip -o build/distributions/jobt-$VERSION.zip -d $TARGET
