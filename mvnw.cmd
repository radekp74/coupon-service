@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "BASE_DIR=%~dp0"
set "PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

if not exist "%PROPERTIES%" (
  echo ERROR: missing %PROPERTIES% 1>&2
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in (%PROPERTIES%) do (
  if "%%A"=="distributionUrl" set "DISTRIBUTION_URL=%%B"
  if "%%A"=="distributionSha512Sum" set "EXPECTED_SHA512=%%B"
)

set "MAVEN_VERSION=3.9.16"
if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "INSTALL_ROOT=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%INSTALL_ROOT%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MAVEN_BIN%" (
  where powershell.exe >nul 2>&1 || (
    echo ERROR: PowerShell is required to install Maven Wrapper distribution 1>&2
    exit /b 1
  )
  if not exist "%INSTALL_ROOT%" mkdir "%INSTALL_ROOT%"
  set "ARCHIVE=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing '%DISTRIBUTION_URL%' -OutFile '%ARCHIVE%'; $actual=(Get-FileHash -Algorithm SHA512 '%ARCHIVE%').Hash.ToLowerInvariant(); if ($actual -ne '%EXPECTED_SHA512%') { throw ('Maven distribution SHA-512 mismatch. Actual: ' + $actual) }; if (Test-Path '%MAVEN_HOME%') { Remove-Item -Recurse -Force '%MAVEN_HOME%' }; Expand-Archive -Force '%ARCHIVE%' '%INSTALL_ROOT%'"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
