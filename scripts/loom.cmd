@echo off

setlocal

set VERSION=@LOOM_VERSION@

rem Find project directory of executed loom script
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set PROJECT_DIR=%DIRNAME%

rem Find the java executable
if defined JAVA_HOME goto configure_via_java_home

set JAVACMD=java.exe
%JAVACMD% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" (
    echo Warning: JAVA_HOME environment variable is not set - using %JAVACMD% from path
    goto define_lib
)

echo ERROR: Can't find Java - JAVA_HOME is not set and no java was found in your PATH

goto error

:configure_via_java_home
set JAVA_HOME=%JAVA_HOME:"=%
set JAVACMD=%JAVA_HOME%\bin\java.exe

if exist "%JAVACMD%" goto define_lib

echo ERROR: Can't execute %JAVACMD%
echo Please ensure JAVA_HOME is configured correctly: %JAVA_HOME%

goto error

:define_lib
if defined LOOM_USER_HOME goto :init
set LOOM_USER_HOME=%LOCALAPPDATA%\Loom\Loom

:init
set LIB=%LOOM_USER_HOME%\library\loom-%VERSION%\lib\loom-%VERSION%.jar
if exist %LIB% %JAVA_CMD% goto launch

rem download Loom Installer
"%JAVACMD%" -jar %PROJECT_DIR%\loom-installer\loom-installer.jar %PROJECT_DIR%
if ERRORLEVEL 1 goto error

:launch
rem run Loom
"%JAVACMD%" %LOOM_OPTS% -Dloom.project_dir=%PROJECT_DIR% -jar %LIB% %*
if ERRORLEVEL 1 goto error
goto end

:error
rem Set LOOM_EXIT_CONSOLE to exit the CMD and not only this script
if not "%LOOM_EXIT_CONSOLE%" == "" exit 1
exit /B 1

:end
endlocal
