@echo off
chcp 65001 >nul
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 set "PATH=C:\Users\TR\tools\apache-maven-3.9.16\bin;%PATH%"

call mvn test
echo.
echo Coverage report: target\site\jacoco\index.html
echo.
pause
