@echo off
setlocal

cd /d "%~dp0"

if "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME is not set!
    echo Please set JAVA_HOME to your JDK 21+ directory.
    echo Example: set JAVA_HOME=C:\ADev\lang\java\jdk21
    exit /b 1
)

:: Strip trailing backslash if present
if "%JAVA_HOME:~-1%"=="\" set "JAVA_HOME=%JAVA_HOME:~0,-1%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JAVA_HOME is set to "%JAVA_HOME%", but java.exe was not found.
    echo Please verify that JAVA_HOME points to a valid JDK installation directory.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "target\file-search-pro-1.0.0.jar" (
    echo [FileSearch Pro] Jar not found, building first...
    call mvn package -DskipTests
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Build failed!
        exit /b %ERRORLEVEL%
    )
)

start "" javaw -jar target\file-search-pro-1.0.0.jar %*
endlocal
