$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$dashboard = Join-Path $root "dashboard"
$port = if ($args.Count -gt 0) { [int]$args[0] } else { 5500 }
$url = "http://localhost:$port"

if (-not (Test-Path (Join-Path $dashboard "index.html"))) {
    throw "Dashboard index.html was not found at $dashboard"
}

$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    $python = Get-Command python -ErrorAction SilentlyContinue
    $pyLauncher = Get-Command py -ErrorAction SilentlyContinue

    if ($python) {
        Start-Process -FilePath $python.Source `
            -ArgumentList "-m http.server $port --bind 127.0.0.1 --directory `"$dashboard`"" `
            -WindowStyle Hidden
    } elseif ($pyLauncher) {
        Start-Process -FilePath $pyLauncher.Source `
            -ArgumentList "-3 -m http.server $port --bind 127.0.0.1 --directory `"$dashboard`"" `
            -WindowStyle Hidden
    } else {
        throw "Python was not found. Install Python or open dashboard\index.html directly in your browser."
    }

    Start-Sleep -Seconds 1
}

Start-Process $url
Write-Host "AegisCloud dashboard is running at $url"
