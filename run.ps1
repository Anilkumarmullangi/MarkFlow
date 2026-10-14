$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java was not found. Install JDK 21 and reopen PowerShell. See BUILD_AND_RUN.md."
}
$port = if ($env:PORT) { $env:PORT } else { "8080" }
Write-Host "Starting Markdown to HTML Converter Pro on http://localhost:$port (press Ctrl+C to stop)"
& "$root\backend\mvnw.cmd" -f "$root\backend\pom.xml" spring-boot:run
exit $LASTEXITCODE
