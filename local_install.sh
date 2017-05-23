#!/bin/sh
./gradlew clean zip
unzip -o build/distributions/jobt-1.0.0.zip -d ~/.jobt/binary/1.0.0/

