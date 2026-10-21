@echo off
chcp 65001 >nul
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
    echo [error] Maven 3.9+ was not found. Install Maven or add it to PATH.
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [error] Java 21 was not found. Install a JDK and add it to PATH.
    pause
    exit /b 1
)

echo [build] Packaging the current source...
call mvn -q clean package -DskipTests
if errorlevel 1 (
    echo [error] Build failed. Run test.bat to see details.
    pause
    exit /b 1
)

java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar target\ohcp-app.jar
echo.
pause
