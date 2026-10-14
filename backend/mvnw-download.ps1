param(
    [string]$MavenVersion = "3.9.11"
)
$ErrorActionPreference = "Stop"
$wrapperRoot = Join-Path $PSScriptRoot ".mvn\wrapper"
$mavenHome = Join-Path $wrapperRoot "apache-maven-$MavenVersion"
$mavenZip = Join-Path $wrapperRoot "apache-maven-$MavenVersion-bin.zip"
$downloadUrl = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"

New-Item -ItemType Directory -Force -Path $wrapperRoot | Out-Null
if (-not (Test-Path (Join-Path $mavenHome "bin\mvn.cmd"))) {
    Write-Host "Downloading Apache Maven $MavenVersion (one-time setup)..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $mavenZip
    Expand-Archive -Path $mavenZip -DestinationPath $wrapperRoot -Force
    Remove-Item $mavenZip -Force -ErrorAction SilentlyContinue
}
if (-not (Test-Path (Join-Path $mavenHome "bin\mvn.cmd"))) {
    throw "Maven extraction did not produce the expected bin\mvn.cmd file."
}
Write-Host "Maven is ready."
