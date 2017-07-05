@ECHO OFF
set VERSION=1.0.0
set LIB=%HOMEDRIVE%%HOMEPATH%\.loom\binary\loom-%VERSION%\lib\loom-%VERSION%.jar

IF NOT EXIST %LIB% java -cp loom-downloader\loom-downloader.jar builders.loom.downloader.LoomDownloader

java -jar %LIB% %*
