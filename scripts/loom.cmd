@ECHO OFF
set VERSION=1.0.0
set LIB=%HOMEDRIVE%%HOMEPATH%\.loom\binary\%VERSION%\lib\loom-%VERSION%.jar

IF NOT EXIST %LIB% java -cp loom-downloader\loom-downloader.jar builders.loom.LoomDownloader %VERSION%

java -jar %LIB% %*
