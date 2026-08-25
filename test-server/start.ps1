$Host.UI.RawUI.WindowTitle = "PlayerHeadShop Test Server (Paper 1.21.4)"
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Starting PlayerHeadShop Paper 1.21.4 Test Server..." -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

$javaCmd = "java"
if (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe") {
    $javaCmd = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\java.exe"
}

& $javaCmd -Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -jar "$PSScriptRoot\paper.jar" --nogui
