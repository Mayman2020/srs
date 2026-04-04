<#
.SYNOPSIS
    Build and run the admin-communications Spring Boot backend (restart-safe).

.DESCRIPTION
    Production-style local startup script that:
    - Stops any existing Java process listening on the API port (default 8081; avoids Oracle TNSLSNR on 8080)
    - Resolves JDK 17 (JAVA_HOME or common install paths)
    - Runs Maven clean install (optional skip)
    - Starts Spring Boot with optional Spring profile (e.g. local)

.PARAMETER Profile
    Spring profile (e.g. "local" for application-local.yml). Default "" = if application-local.yml exists, "local" is used automatically; otherwise application.yml + env vars.

.PARAMETER SkipBuild
    Skip Maven build; only run the application.

.PARAMETER Port
    Server port to free before start and to show in URLs. Default 8081 (8080 often taken by Oracle TNSLSNR). Use -Port 8080 if free.

.EXAMPLE
    .\run-backend.ps1
    .\run-backend.ps1 -SkipBuild
    .\run-backend.ps1 -Profile local
    .\run-backend.ps1 -Port 8080
#>

[CmdletBinding()]
param(
    [string]$Profile = "",
    [switch]$SkipBuild,
    [int]$Port = 8081
)

$ErrorActionPreference = 'Stop'
$DefaultPort = $Port
$ExpectedProcess = "java"
$ApiPrefix = "/api/v1"
$BaseUrl = "http://localhost:$DefaultPort$ApiPrefix"

# Script root = backend module (this folder)
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$ProjectRoot = $ScriptDir
$MvnwPath = Join-Path $ProjectRoot "mvnw.cmd"
$SecretsFile = Join-Path $ProjectRoot "run-backend.secrets.ps1"
$LocalYml = Join-Path $ProjectRoot "src\main\resources\application-local.yml"

# Logging helpers
function Write-Step {
    param([string]$Message, [string]$Color = "Cyan")
    $ts = Get-Date -Format "HH:mm:ss"
    Write-Host "[$ts] " -NoNewline
    Write-Host $Message -ForegroundColor $Color
}
function Write-Success { param([string]$Message) Write-Step $Message "Green" }
function Write-Warn { param([string]$Message) Write-Step $Message "Yellow" }
function Write-Err { param([string]$Message) Write-Step $Message "Red" }
function Write-Info { param([string]$Message) Write-Step $Message "Gray" }

# Prefer local profile when the gitignored file exists (avoids missing DB password / JWT when double-clicking run-backend.cmd)
if ([string]::IsNullOrWhiteSpace($Profile) -and (Test-Path $LocalYml)) {
    $Profile = "local"
    Write-Info "Spring profile: local (found application-local.yml)."
}
$useLocalYml = ($Profile -eq "local") -and (Test-Path $LocalYml)

# Local secrets (not committed). Example: run-backend.secrets.EXAMPLE.ps1 -> run-backend.secrets.ps1
#   $env:AC_JWT_SECRET = '...'   (>= 32 UTF-8 bytes)
#   $env:SPRING_DATASOURCE_PASSWORD = 'your_postgres_password'   # required for SCRAM unless using -Profile local + application-local.yml
#   $env:CAMUNDA_BPM_ADMIN_PASSWORD = 'admin'
$jwtWeak = (-not $env:AC_JWT_SECRET) -or ($env:AC_JWT_SECRET.Trim().Length -lt 32)
$dbPassUnset = [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)
if ((Test-Path $SecretsFile) -and ($jwtWeak -or $dbPassUnset)) {
    Write-Info "Loading local secrets: $SecretsFile"
    . $SecretsFile
    $jwtWeak = (-not $env:AC_JWT_SECRET) -or ($env:AC_JWT_SECRET.Trim().Length -lt 32)
    $dbPassUnset = [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)
}
if ($jwtWeak -and -not $useLocalYml) {
    Write-Warn "AC_JWT_SECRET is missing or shorter than 32 bytes. Spring Boot will fail JWT setup."
    Write-Info "  Fix: create run-backend.secrets.ps1 (gitignored) or set AC_JWT_SECRET before running."
    Write-Info "  Example: `$env:AC_JWT_SECRET = '0123456789abcdef0123456789abcdef0123456789abcdef01'"
}

if ($dbPassUnset -and (-not $useLocalYml)) {
    Write-Err "SPRING_DATASOURCE_PASSWORD is not set. PostgreSQL answered: SCRAM authentication - a password is required."
    Write-Info "  Fix A: copy run-backend.secrets.EXAMPLE.ps1 to run-backend.secrets.ps1 and set SPRING_DATASOURCE_PASSWORD (and AC_JWT_SECRET)."
    Write-Info "  Fix B: copy application-local.example.yml to application-local.yml, set spring.datasource.password; run-backend then uses profile local automatically."
    exit 1
}
if ($Profile -eq 'local' -and -not (Test-Path $LocalYml)) {
    Write-Err "Profile 'local' requires $LocalYml (copy from application-local.example.yml)."
    exit 1
}

# Port and process (restart behavior)
function Get-ProcessOnPort {
    param([int]$PortListen)
    try {
        $conn = Get-NetTCPConnection -LocalPort $PortListen -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
            return @{ Process = $proc; Connection = $conn }
        }
    } catch { }
    try {
        $line = netstat -ano 2>$null | Select-String ":$PortListen\s+.*LISTENING" | Select-Object -First 1
        if ($line) {
            $parts = ($line -split '\s+')
            $pidVal = $parts[-1]
            if ($pidVal -match '^\d+$') {
                $proc = Get-Process -Id $pidVal -ErrorAction SilentlyContinue
                if ($proc) { return @{ Process = $proc } }
            }
        }
    } catch { }
    return $null
}

function Stop-ProcessOnPort {
    param([int]$PortListen, [string]$ExpectedName)
    $found = Get-ProcessOnPort -PortListen $PortListen
    if (-not $found) {
        Write-Info "Port $PortListen is free."
        return $true
    }
    $proc = $found.Process
    $pidVal = $proc.Id
    $procName = $proc.ProcessName
    $match = $procName -like "*$ExpectedName*"
    if (-not $match) {
        Write-Warn "Port $PortListen is in use by $procName (PID $pidVal), not $ExpectedName. Skipping kill for safety."
        if ($procName -match 'TNSLSNR|oracle') {
            Write-Info "  (Oracle listener often uses 8080. Use .\run-backend.ps1 -Port 8081 and proxy.conf.json target http://localhost:8081.)"
        }
        return $false
    }
    Write-Step "Port $PortListen is in use by $procName (PID $pidVal) -> stopping process..." "Yellow"
    try {
        Stop-Process -Id $pidVal -Force -ErrorAction Stop
    } catch {
        Write-Err "Failed to stop process: $_"
        return $false
    }
    Start-Sleep -Seconds 2
    if (Get-ProcessOnPort -PortListen $PortListen) {
        Write-Err "Process stopped but port $PortListen still in use."
        return $false
    }
    Write-Success "Process stopped successfully."
    return $true
}

# Java setup
$JavaCandidates = @(
    $env:JAVA_HOME,
    "D:\Progs\Progs Work\jdk_17_new_java",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Eclipse Adoptium\jdk-17*",
    "C:\Program Files\Microsoft\jdk-17*"
)
$ResolvedJavaHome = $null
foreach ($candidate in $JavaCandidates) {
    if (-not $candidate) { continue }
    $path = if ($candidate -match '\*') { (Get-Item $candidate -ErrorAction SilentlyContinue | Select-Object -First 1).FullName } else { $candidate }
    if ($path -and (Test-Path $path) -and (Test-Path (Join-Path $path "bin\java.exe"))) {
        $ResolvedJavaHome = $path
        break
    }
}
if (-not $ResolvedJavaHome) {
    Write-Err "JAVA_HOME not found. Set JAVA_HOME or install JDK 17."
    exit 1
}
$env:JAVA_HOME = $ResolvedJavaHome
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
Write-Step "Java configured: $ResolvedJavaHome" "Cyan"
$prevErr = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& java -version 2>&1 | ForEach-Object { Write-Info "  $_" }
$ErrorActionPreference = $prevErr

# Maven wrapper (required)
if (-not (Test-Path $MvnwPath)) {
    Write-Err "Maven wrapper not found: $MvnwPath"
    exit 1
}
Set-Location $ProjectRoot

# Pre-flight: PostgreSQL - ensure DB exists
$DbName = "ac_communications"
$DbUser = 'postgres'
if ($env:SPRING_DATASOURCE_USERNAME) { $DbUser = $env:SPRING_DATASOURCE_USERNAME }
$pgPort = 5432
try {
    $pgListen = Get-NetTCPConnection -LocalPort $pgPort -State Listen -ErrorAction SilentlyContinue
    if (-not $pgListen) {
        Write-Warn "PostgreSQL does not appear to be listening on port $pgPort. Start PostgreSQL before running."
    }
} catch { }

$psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlCmd) {
    $dbCheck = & psql -U $DbUser -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='$DbName'" 2>$null
    if ($dbCheck -and $dbCheck.Trim() -eq '1') {
        Write-Info "Database '$DbName' already exists."
    } else {
        Write-Step "Creating database '$DbName'..." "Cyan"
        & psql -U $DbUser -d postgres -c "CREATE DATABASE $DbName" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Database '$DbName' created."
        } else {
            Write-Info "psql CREATE DATABASE returned non-zero; Spring Boot will auto-create on startup."
        }
    }
} else {
    Write-Info "psql not on PATH - Spring Boot will auto-create database if missing."
}

# Restart: stop old backend on port
Write-Step "Checking port $DefaultPort..." "Cyan"
if (-not (Stop-ProcessOnPort -PortListen $DefaultPort -ExpectedName $ExpectedProcess)) {
    Write-Err "Pick a free port, e.g. .\run-backend.ps1 -Port 8081 (default). If you change the port, set SRS_System_frontend\proxy.conf.json `"target`" to the same URL."
    exit 1
}

# SERVER_PORT for this run
$env:SERVER_PORT = "$DefaultPort"

# Maven build
if (-not $SkipBuild) {
    Write-Step "Maven build started..." "Cyan"
    & $MvnwPath clean install -U
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Maven build FAILED."
        exit $LASTEXITCODE
    }
    Write-Success "Maven build finished successfully."
} else {
    Write-Info "Skipping build (-SkipBuild)."
}

# Start backend
if ($Profile) {
    $env:SPRING_PROFILES_ACTIVE = $Profile
    Write-Step "Starting backend (profile: $Profile)..." "Cyan"
} else {
    if (Test-Path Env:SPRING_PROFILES_ACTIVE) { Remove-Item Env:SPRING_PROFILES_ACTIVE }
    Write-Step "Starting backend (default config + environment variables)..." "Cyan"
}
Write-Info "  API base: $BaseUrl"
Write-Info "  Swagger UI: http://localhost:$DefaultPort/swagger-ui.html"
Write-Info "  Database (default URL): ${DbName} at localhost:${pgPort}"
Write-Info "  Stop with Ctrl+C"
Write-Host ""

$runArgs = @("spring-boot:run")
if ($Profile) {
    $runArgs += "-Dspring-boot.run.profiles=$Profile"
}

& $MvnwPath @runArgs
$exitCode = $LASTEXITCODE

Write-Host ""
if ($exitCode -eq 0) {
    Write-Success "Backend stopped normally."
} else {
    Write-Err "Backend exited with failure (exit code $exitCode)."
    Write-Info "Common causes: DB password missing (SCRAM), wrong password, AC_JWT_SECRET unset, port in use, or invalid BPMN/Camunda config."
}
exit $exitCode
