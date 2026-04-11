<#
.SYNOPSIS
    Build and run the admin-communications Spring Boot backend (restart-safe) in LOCAL mode by default.

.DESCRIPTION
    Developer-first defaults:
    - Sets SPRING_PROFILES_ACTIVE=local (override with -Profile for staging/prod experiments)
    - Default port 8080 (matches SRS_System_frontend\proxy.conf.json)
    - Ensures application-local.yml exists (copies from application-local.example.yml if missing)
    - Stops any existing Java process listening on the API port
    - Resolves JDK 17, runs Maven (optional skip)

.PARAMETER Profile
    Spring profile. Default is local (application-local.yml). Use staging or prod only when you intend to.

.PARAMETER SkipBuild
    Skip Maven build; only run the application.

.PARAMETER Port
    Preferred server port. Default 8080. If something non-Java (e.g. Oracle TNSLSNR) holds this port, the script auto-picks the next free port unless -NoAutoPort is set.

.PARAMETER NoAutoPort
    If the preferred port is blocked by a non-Java process, exit with an error instead of searching for a free port.

.PARAMETER NoProxyUpdate
    When the script auto-selects a different port, it normally updates SRS_System_frontend\proxy.conf.json. Use this switch to skip that file change.

.EXAMPLE
    .\run-backend.ps1
    .\run-backend.ps1 -SkipBuild
    .\run-backend.ps1 -Port 8081
    .\run-backend.ps1 -NoAutoPort
#>

[CmdletBinding()]
param(
    [string]$Profile = "local",
    [switch]$SkipBuild,
    [int]$Port = 8080,
    [switch]$NoAutoPort,
    [switch]$NoProxyUpdate
)

$ErrorActionPreference = 'Stop'
$ExpectedProcess = "java"
$ApiPrefix = "/api/v1"

# Script root = backend module (this folder)
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$ProjectRoot = $ScriptDir
$MvnwPath = Join-Path $ProjectRoot "mvnw.cmd"
$SecretsFile = Join-Path $ProjectRoot "run-backend.secrets.ps1"
$LocalYml = Join-Path $ProjectRoot "src\main\resources\application-local.yml"
$ExampleYml = Join-Path $ProjectRoot "src\main\resources\application-local.example.yml"

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

if ($Profile -eq 'local' -and -not (Test-Path $LocalYml)) {
    if (Test-Path $ExampleYml) {
        Copy-Item -Path $ExampleYml -Destination $LocalYml -Force
        Write-Success "Created application-local.yml from application-local.example.yml (LOCAL defaults)."
    } else {
        Write-Err "Profile 'local' requires $LocalYml and template $ExampleYml is missing."
        exit 1
    }
}

$useLocalYml = ($Profile -eq "local") -and (Test-Path $LocalYml)

# Optional overrides (gitignored): run-backend.secrets.ps1
# Profile local: always load when present so AC_* applies even if Windows defines SPRING_DATASOURCE_PASSWORD (cleared below).
if ($Profile -eq 'local' -and (Test-Path $SecretsFile)) {
    Write-Info "Loading local secrets: $SecretsFile"
    . $SecretsFile
}

$jwtWeak = $false
if ($env:AC_JWT_SECRET -and $env:AC_JWT_SECRET.Trim().Length -lt 32) { $jwtWeak = $true }
$dbPassUnset = [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD) -and [string]::IsNullOrWhiteSpace($env:AC_LOCAL_DB_PASSWORD) -and [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)
if ($Profile -ne 'local' -and (Test-Path $SecretsFile) -and ($jwtWeak -or $dbPassUnset)) {
    Write-Info "Loading local secrets: $SecretsFile"
    . $SecretsFile
    $jwtWeak = ($env:AC_JWT_SECRET -and $env:AC_JWT_SECRET.Trim().Length -lt 32)
    $dbPassUnset = [string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD) -and [string]::IsNullOrWhiteSpace($env:AC_LOCAL_DB_PASSWORD) -and [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)
}
if ($jwtWeak) {
    Write-Warn "AC_JWT_SECRET is set but shorter than 32 bytes. Spring Boot will fail JWT setup."
    Write-Info "  Unset AC_JWT_SECRET to use application-local.yml dev secret, or set a 32+ byte value."
}
if ($dbPassUnset -and (-not $useLocalYml)) {
    Write-Err "SPRING_DATASOURCE_PASSWORD is not set and profile is not 'local' (no application-local.yml merge)."
    Write-Info "  Fix: use -Profile local, or set SPRING_DATASOURCE_PASSWORD / run-backend.secrets.ps1"
    exit 1
}

Write-Step "LOCAL mode: SPRING_PROFILES_ACTIVE=$Profile | API (preferred) http://localhost:$Port$ApiPrefix" "Cyan"

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

function Test-PortInUse {
    param([int]$PortListen)
    return $null -ne (Get-ProcessOnPort -PortListen $PortListen)
}

# Next free TCP listen port in [start, start + maxAttempts).
function Find-NextFreePort {
    param(
        [int]$StartPort,
        [int]$MaxAttempts = 40
    )
    for ($i = 0; $i -lt $MaxAttempts; $i++) {
        $p = $StartPort + $i
        if (-not (Test-PortInUse -PortListen $p)) {
            return $p
        }
    }
    return $null
}

function Update-FrontendProxyTarget {
    param(
        [int]$ListenPort,
        [string]$ProxyFile
    )
    if (-not (Test-Path $ProxyFile)) {
        Write-Warn "Frontend proxy file not found: $ProxyFile (skipping update)."
        return
    }
    try {
        $raw = Get-Content -Path $ProxyFile -Raw -Encoding UTF8
        $rx = '("target"\s*:\s*)"http://localhost:\d+"'
        $replacement = '$1"http://localhost:{0}"' -f $ListenPort
        $updated = $raw -replace $rx, $replacement
        if ($updated -eq $raw) {
            Write-Warn "Could not find proxy target pattern in $ProxyFile - set target to http://localhost:$ListenPort manually."
            return
        }
        # UTF-8 without BOM — Angular's proxy JSON parser rejects EF BB BF at position 0.
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($ProxyFile, $updated.TrimEnd() + "`n", $utf8NoBom)
        Write-Success "Updated frontend proxy: $ProxyFile -> http://localhost:$ListenPort"
    } catch {
        Write-Warn "Failed to update proxy file: $_"
    }
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
            Write-Info '  (Oracle listener often uses 8080 - script will try the next free port unless -NoAutoPort.)'
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

# Pre-flight: PostgreSQL - catalogue postgres; all app + Camunda tables live in schema srs_system only (same DB URL pattern in DBeaver).
$DbName = "postgres"
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
    Write-Info "Expecting JDBC database 'postgres', schema 'srs_system' (override with SPRING_DATASOURCE_URL if needed)."
}
else {
    Write-Info "psql not on PATH - ensure PostgreSQL is running; default URL uses database postgres + schema srs_system."
}
Write-Info 'DBeaver: same host/port/DB as this run; JDBC URL should include currentSchema=srs_system (or set search_path). Otherwise table lists may look incomplete.'

if ($Profile -eq 'local') {
    # OS-wide SPRING_DATASOURCE_PASSWORD overrides YAML and often breaks local (wrong/empty).
    if (Test-Path Env:SPRING_DATASOURCE_PASSWORD) {
        Remove-Item Env:\SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
        Write-Info "Local profile: cleared SPRING_DATASOURCE_PASSWORD (use AC_LOCAL_DB_PASSWORD or DB_PASSWORD in run-backend.secrets.ps1 if auth fails)."
    }
}

# Restart: stop old Java on preferred port, or auto-pick next free port if non-Java holds it
$chosenPort = $Port
$ProxyJson = Join-Path $ProjectRoot "..\SRS_System_frontend\proxy.conf.json"

Write-Step "Checking port $chosenPort..." "Cyan"
if (-not (Stop-ProcessOnPort -PortListen $chosenPort -ExpectedName $ExpectedProcess)) {
    if ($NoAutoPort) {
        Write-Err "Port $chosenPort is not available. Use .\run-backend.ps1 -Port 8081 (or another free port), or omit -NoAutoPort to auto-pick."
        Write-Info "  If you change the port manually, set SRS_System_frontend\proxy.conf.json `"target`" to the same URL."
        exit 1
    }
    Write-Warn "Port $chosenPort blocked by a non-Java process - searching for a free port..."
    $next = Find-NextFreePort -StartPort ($chosenPort + 1)
    if (-not $next) {
        Write-Err "No free port found in range starting at $($chosenPort + 1). Free a port or pass -Port explicitly."
        exit 1
    }
    $chosenPort = $next
    Write-Success "Using SERVER_PORT=$chosenPort (API http://localhost:$chosenPort$ApiPrefix)"
    if (-not $NoProxyUpdate) {
        Update-FrontendProxyTarget -ListenPort $chosenPort -ProxyFile $ProxyJson
    } else {
        Write-Warn "Skipped proxy update (-NoProxyUpdate). Set frontend proxy target to http://localhost:$chosenPort"
    }
}

$DefaultPort = $chosenPort
$BaseUrl = "http://localhost:$DefaultPort$ApiPrefix"

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

# Start backend — always set active profile for this process (LOCAL default = local)
$env:SPRING_PROFILES_ACTIVE = $Profile
Write-Step "Starting Spring Boot (spring.profiles.active=$Profile)..." "Cyan"
Write-Info "  API base: $BaseUrl"
Write-Info "  Swagger UI: http://localhost:$DefaultPort/swagger-ui.html"
Write-Info "  Actuator: http://localhost:$DefaultPort/actuator/health"
Write-Info "  Database (default URL): ${DbName} at localhost:${pgPort}"
Write-Info "  Stop with Ctrl+C"
Write-Host ""

$runArgs = @("spring-boot:run", "-Dspring-boot.run.profiles=$Profile")

$prevSpringAppJson = $null
if ($Profile -eq 'local') {
    # Force datasource password via JSON (high precedence) so Windows env / IDE pollution cannot break startup.
    $localPw = $env:AC_LOCAL_DB_PASSWORD
    if ([string]::IsNullOrWhiteSpace($localPw)) { $localPw = $env:DB_PASSWORD }
    if ([string]::IsNullOrWhiteSpace($localPw)) {
        $localPw = 'admin'
    } else {
        $localPw = $localPw.Trim()
    }
    $esc = $localPw -replace '\\', '\\' -replace '"', '\"'
    if (Test-Path Env:SPRING_APPLICATION_JSON) {
        $prevSpringAppJson = $env:SPRING_APPLICATION_JSON
    }
    $env:SPRING_APPLICATION_JSON = "{`"spring`":{`"datasource`":{`"password`":`"$esc`"}}}"
    Write-Info "Local DB password applied via SPRING_APPLICATION_JSON (length $($localPw.Length); set AC_LOCAL_DB_PASSWORD or DB_PASSWORD if auth fails)."
}

$exitCode = 1
try {
    & $MvnwPath @runArgs
    $exitCode = $LASTEXITCODE
} finally {
    if ($Profile -eq 'local') {
        Remove-Item Env:\SPRING_APPLICATION_JSON -ErrorAction SilentlyContinue
        if ($null -ne $prevSpringAppJson) {
            $env:SPRING_APPLICATION_JSON = $prevSpringAppJson
        }
    }
}

Write-Host ""
if ($exitCode -eq 0) {
    Write-Success "Backend stopped normally."
} else {
    Write-Err "Backend exited with failure (exit code $exitCode)."
    Write-Info "Common causes: DB password missing (SCRAM), wrong password, AC_JWT_SECRET unset, port in use, or invalid BPMN/Camunda config."
    if ($Profile -eq 'local') {
        Write-Info "  Local: set AC_LOCAL_DB_PASSWORD or DB_PASSWORD in run-backend.secrets.ps1 to match PostgreSQL user (hesabaty default is admin)."
    }
}
exit $exitCode
