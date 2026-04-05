$ErrorActionPreference = "Stop"
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
Set-Location $ScriptDir

Write-Host "[run-frontend] LOCAL development: ng serve --configuration=development (proxy /api -> http://localhost:8080)" -ForegroundColor Cyan

if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found. Installing packages..." -ForegroundColor Yellow
    npm install
}

npm run start
