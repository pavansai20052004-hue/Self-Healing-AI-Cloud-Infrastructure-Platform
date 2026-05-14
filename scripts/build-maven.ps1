$ErrorActionPreference = "Stop"

$jdk = "C:\Program Files\Java\jdk-21.0.11"
if (Test-Path $jdk) {
    $env:JAVA_HOME = $jdk
}

& mvn -DskipTests package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
