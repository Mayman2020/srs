@echo off
cd /d "%~dp0"
echo Starting backend from: %CD%
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-backend.ps1" %*
echo.
pause
