@echo off
chcp 65001 >nul
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 set "PATH=C:\Users\TR\tools\apache-maven-3.9.16\bin;%PATH%"

if not exist "target\ohcp-app.jar" (
    echo [build] target\ohcp-app.jar not found, packaging...
    call mvn -q package -DskipTests
    if errorlevel 1 (
        echo [error] build failed. run test.bat to see details.
        pause
        exit /b 1
    )
)

java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar target\ohcp-app.jar
echo.
pause
