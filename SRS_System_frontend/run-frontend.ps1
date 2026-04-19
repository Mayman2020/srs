$ErrorActionPreference = "Stop"
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
Set-Location $ScriptDir

$proxyConfigPath = Join-Path $ScriptDir "proxy.conf.json"
$proxyTarget = $null

if (Test-Path $proxyConfigPath) {
    try {
        $proxyConfig = Get-Content $proxyConfigPath -Raw | ConvertFrom-Json
        $proxyTarget = $proxyConfig.'/api'.target
    } catch {
        Write-Host "[run-frontend] Warning: could not parse proxy.conf.json: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

if ([string]::IsNullOrWhiteSpace($proxyTarget)) {
    $proxyTarget = "http://localhost:8080"
}

Write-Host "[run-frontend] LOCAL development: ng serve --configuration=development (proxy /api -> $proxyTarget)" -ForegroundColor Cyan

try {
    $proxyUri = [Uri]$proxyTarget
    $port = if ($proxyUri.IsDefaultPort) {
        if ($proxyUri.Scheme -eq "https") { 443 } else { 80 }
    } else {
        $proxyUri.Port
    }
    $reachable = Test-NetConnection -ComputerName $proxyUri.Host -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
    if (-not $reachable) {
        Write-Host "[run-frontend] Warning: backend proxy target is not reachable right now. Vite will log ECONNREFUSED for /api requests until that server starts." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[run-frontend] Warning: could not verify backend proxy target '$proxyTarget'." -ForegroundColor Yellow
}

if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found. Installing packages..." -ForegroundColor Yellow
    npm install
}

npm run start
