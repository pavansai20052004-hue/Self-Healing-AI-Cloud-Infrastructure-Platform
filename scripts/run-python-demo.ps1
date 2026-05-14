$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location (Join-Path $root "ai-prediction-service")
try {
    & python local_demo.py
}
finally {
    Pop-Location
}

