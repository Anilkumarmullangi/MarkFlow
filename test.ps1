$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java was not found. Install JDK 21 and reopen PowerShell. See BUILD_AND_RUN.md."
}
& "$root\backend\mvnw.cmd" -f "$root\backend\pom.xml" clean test
exit $LASTEXITCODE
