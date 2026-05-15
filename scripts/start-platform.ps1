param(
    [switch] $Rebuild,
    [switch] $Restart
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$logDir = Join-Path $root ".build\logs"
New-Item -ItemType Directory -Force $logDir | Out-Null

function Stop-PortProcess {
    param([int] $Port)
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $process.Id -Force
            Write-Host "Stopped process $($process.Id) on port $Port"
        }
    }
}

function Start-JavaService {
    param(
        [string] $Name,
        [string] $Jar,
        [int] $Port
    )
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "$Name already running on port $Port"
        return
    }

    Start-Process -FilePath "java" `
        -ArgumentList "-jar `"$Jar`"" `
        -WorkingDirectory $root `
        -RedirectStandardOutput (Join-Path $logDir "$Name.out.log") `
        -RedirectStandardError (Join-Path $logDir "$Name.err.log") `
        -WindowStyle Hidden
    Write-Host "Started $Name on port $Port"
}

function Resolve-Python {
    $bundled = Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
    if (Test-Path $bundled) {
        return $bundled
    }
    $systemPython = Get-Command python -ErrorAction SilentlyContinue
    if ($systemPython) {
        return $systemPython.Source
    }
    $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
    if ($pyLauncher) {
        return $pyLauncher.Source
    }
    throw "Python was not found. Install Python 3.12+ and retry."
}

if ($Restart) {
    5500, 8081, 8082, 8083, 8090 | ForEach-Object { Stop-PortProcess -Port $_ }
}

$jars = @(
    (Join-Path $root "monitoring-service\target\monitoring-service-0.1.0-SNAPSHOT.jar"),
    (Join-Path $root "healing-engine\target\healing-engine-0.1.0-SNAPSHOT.jar"),
    (Join-Path $root "incident-intelligence-service\target\incident-intelligence-service-0.1.0-SNAPSHOT.jar")
)

if ($Rebuild -or ($jars | Where-Object { -not (Test-Path $_) })) {
    & (Join-Path $PSScriptRoot "build-maven.ps1")
}

Start-JavaService -Name "monitoring-service" -Jar $jars[0] -Port 8081
Start-JavaService -Name "healing-engine" -Jar $jars[1] -Port 8082
Start-JavaService -Name "incident-intelligence-service" -Jar $jars[2] -Port 8083

$predictionRoot = Join-Path $root "ai-prediction-service"
$venvPython = Join-Path $predictionRoot ".venv\Scripts\python.exe"
if (-not (Test-Path $venvPython)) {
    $python = Resolve-Python
    & $python -m venv (Join-Path $predictionRoot ".venv")
}

& $venvPython -m pip install -r (Join-Path $predictionRoot "requirements.txt") | Out-Host
if (-not (Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $venvPython `
        -ArgumentList "-m uvicorn app.main:app --host 127.0.0.1 --port 8090" `
        -WorkingDirectory $predictionRoot `
        -RedirectStandardOutput (Join-Path $logDir "ai-prediction-service.out.log") `
        -RedirectStandardError (Join-Path $logDir "ai-prediction-service.err.log") `
        -WindowStyle Hidden
    Write-Host "Started ai-prediction-service on port 8090"
}

& (Join-Path $PSScriptRoot "start-dashboard.ps1")

Start-Sleep -Seconds 8

$checks = @(
    "http://localhost:5500",
    "http://localhost:8081/api/v1/healthz",
    "http://localhost:8082/api/v1/heal/healthz",
    "http://localhost:8083/api/v1/incidents/healthz",
    "http://localhost:8090/healthz"
)

foreach ($check in $checks) {
    try {
        $response = Invoke-WebRequest -Uri $check -UseBasicParsing -TimeoutSec 5
        Write-Host "$check -> $($response.StatusCode)"
    } catch {
        Write-Host "$check -> FAILED: $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "AegisCloud UI: http://localhost:5500"
