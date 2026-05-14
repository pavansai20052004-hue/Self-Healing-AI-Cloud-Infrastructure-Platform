$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$sourceRoot = Join-Path $root "platform-core\src\main\java"
$classes = Join-Path $root ".build\classes"

New-Item -ItemType Directory -Force $classes | Out-Null

$sources = Get-ChildItem -Path $sourceRoot -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if (-not $sources) {
    throw "No Java sources found under $sourceRoot"
}

& javac -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
& java -cp $classes com.aegiscloud.core.demo.DemoScenario
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
