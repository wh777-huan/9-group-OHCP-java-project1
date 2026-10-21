@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo ============================================================
echo   农作物缺素症识别与科学施肥建议智能体 - 一键启动
echo   OHCP 一小时智能编程大赛
echo ============================================================

set JAR=%~dp0backend\target\crop-nutrient-agent.jar

rem ---- 定位 Maven：优先便携版，其次系统安装 ----
set MVN_CMD=
if exist "%~dp0tools\apache-maven-3.9.16\bin\mvn.cmd" (
    set "MVN_CMD=%~dp0tools\apache-maven-3.9.16\bin\mvn.cmd"
) else (
    where mvn >nul 2>nul && set "MVN_CMD=mvn"
)

if exist "%JAR%" (
    echo [1/2] 检测到已打包产物，直接启动...
) else (
    if "%MVN_CMD%"=="" (
        echo [错误] 未找到 Maven。请安装 JDK 17+ 与 Maven 后重试，
        echo        或将便携版 Maven 放到 tools\apache-maven-3.9.16 目录。
        pause
        exit /b 1
    )
    echo [1/2] 开始编译（首次需下载依赖，请耐心等待）...
    if "%MVN_CMD%"=="mvn" (
        call mvn -f "%~dp0backend\pom.xml" clean package -DskipTests
    ) else (
        call "%MVN_CMD%" -s "%~dp0tools\maven-settings.xml" -f "%~dp0backend\pom.xml" clean package -DskipTests
    )
    if errorlevel 1 (
        echo.
        echo [错误] 编译失败，请检查 JDK 17+ 是否安装，或查看上方报错信息。
        pause
        exit /b 1
    )
)

echo [2/2] 启动服务并打开浏览器 http://localhost:8080 ...
start "" http://localhost:8080
java -jar "%JAR%"

endlocal
