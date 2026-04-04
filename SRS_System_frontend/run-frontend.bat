@echo off
setlocal
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-frontend.ps1"
echo.
pause
exit /b %ERRORLEVEL%
