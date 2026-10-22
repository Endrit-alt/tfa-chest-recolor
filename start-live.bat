@echo off
setlocal
cd /d "%~dp0"
if /i "%~1"=="--client" goto client
title TFA Chest Recolor - Live Updates
echo Building TFA Chest Recolor...
call gradlew.bat classes --console=plain
if errorlevel 1 goto failed
powershell.exe -NoProfile -Command "if (Get-NetTCPConnection -LocalAddress 127.0.0.1 -LocalPort 5005 -State Listen -ErrorAction SilentlyContinue) { exit 0 }; exit 1"
if not errorlevel 1 goto watch
echo Starting RuneLite in a separate window...
start "TFA Chest Recolor - Client" "%ComSpec%" /d /c ""%~f0" --client"
echo Waiting for the development client...
powershell.exe -NoProfile -Command "$deadline = (Get-Date).AddSeconds(90); do { if (Get-NetTCPConnection -LocalAddress 127.0.0.1 -LocalPort 5005 -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Seconds 1 } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 goto timeout
:watch
echo Live updates enabled. Leave this window open while editing.
echo Method-body changes reload automatically on save. New fields or methods need a restart.
call gradlew.bat --no-daemon hotSwap --continuous --console=plain
exit /b %errorlevel%

:client
call gradlew.bat run --console=plain
if errorlevel 1 goto failed
exit /b 0

:timeout
echo The client did not open its debug port in time. Check the client window.
echo Once it is ready, run: gradlew.bat hotSwap --continuous
pause
exit /b 1

:failed
echo The command failed. See the error above.
pause
exit /b 1
