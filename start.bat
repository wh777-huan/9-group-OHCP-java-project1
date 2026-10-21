@echo off
chcp 65001 >nul
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 set "PATH=C:\Users\TR\tools\apache-maven-3.9.16\bin;%PATH%"

if not exist "target\ohcp-agri-agent.jar" (
    echo [build] target\ohcp-agri-agent.jar not found, packaging...
    call mvn -q package -DskipTests
    if errorlevel 1 (
        echo [error] build failed.
        pause
        exit /b 1
    )
)

echo 启动中... 浏览器打开 http://localhost:8080
java -Dfile.encoding=UTF-8 -jar target\ohcp-agri-agent.jar
pause
