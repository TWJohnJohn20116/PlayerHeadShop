@echo off
title PlayerHeadShop Test Server (Paper 1.21.4)
echo ========================================================
echo   Starting PlayerHeadShop Paper 1.21.4 Test Server...
echo ========================================================
cd /d "%~dp0"

set "JAVA_CMD=java"
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe"
)

"%JAVA_CMD%" -Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -jar paper.jar --nogui
pause
