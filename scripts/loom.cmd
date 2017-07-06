@ECHO OFF
set VERSION=1.0.0
set LIB=%HOMEDRIVE%%HOMEPATH%\.loom\binary\loom-%VERSION%\lib\loom-%VERSION%.jar

IF NOT EXIST %LIB% java -jar loom-downloader\loom-downloader.jar

java -jar %LIB% %*
