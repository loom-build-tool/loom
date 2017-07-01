@ECHO OFF
set VERSION=1.0.0
set LIB=%HOMEDRIVE%%HOMEPATH%\.jobt\binary\%VERSION%\lib\jobt-%VERSION%.jar

IF NOT EXIST %LIB% java -cp jobt-downloader\jobt-downloader.jar jobt.JobtDownloader %VERSION%

java -jar %LIB% %*
