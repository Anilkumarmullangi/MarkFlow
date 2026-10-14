@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%~dp0.mvn\wrapper\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Maven %MAVEN_VERSION% is not installed for this project. Downloading it automatically...
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0mvnw-download.ps1" -MavenVersion "%MAVEN_VERSION%"
  if errorlevel 1 (
    echo Maven download failed. Check your internet connection and try again.
    exit /b 1
  )
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
