@echo off

setlocal

set VERSION=1.0.0

rem Find the java executable
if defined JAVA_HOME goto configure_via_java_home

set JAVACMD=java.exe
%JAVACMD% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" (
    echo Warning: JAVA_HOME environment variable is not set - using %JAVACMD% from path
    goto init
)

echo ERROR: Can't find Java - JAVA_HOME is not set and no java was found in your PATH

goto error

:configure_via_java_home
set JAVA_HOME=%JAVA_HOME:"=%
set JAVACMD=%JAVA_HOME%\bin\java.exe

if exist "%JAVACMD%" goto init

echo ERROR: Can't execute %JAVACMD%
echo Please ensure JAVA_HOME is configured correctly: %JAVA_HOME%

goto error

:init
set LIB=%LOCALAPPDATA%\Loom\binary\loom-%VERSION%\lib\loom-%VERSION%.jar
if exist %LIB% %JAVA_CMD% goto launch

rem download Loom Installer
"%JAVACMD%" -jar loom-installer\loom-installer.jar
if ERRORLEVEL 1 goto error

:launch
rem run Loom
"%JAVACMD%" %LOOM_OPTS% -jar %LIB% %*
if ERRORLEVEL 1 goto error
goto end

:error
rem Set LOOM_EXIT_CONSOLE to exit the CMD and not only this script
if not "%LOOM_EXIT_CONSOLE%" == "" exit 1
exit /B 1

:end
endlocal
