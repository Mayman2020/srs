@echo off
setlocal
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"
echo Starting backend from: %CD%
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-backend.ps1" %*
echo.
pause
exit /b %ERRORLEVEL%
