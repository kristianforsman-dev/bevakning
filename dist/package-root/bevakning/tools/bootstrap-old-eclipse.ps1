$ErrorActionPreference = "Stop"

Write-Host "=== Bootstrap för gammal Eclipse / offline Maven ==="

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $projectRoot
$offlineRepo = Join-Path $projectRoot "offline-repo"
$localM2 = Join-Path $env:USERPROFILE ".m2"
$localRepo = Join-Path $localM2 "repository"

if (!(Test-Path $offlineRepo)) {
    throw "Hittar inte offline-repo i projektet: $offlineRepo"
}

New-Item -ItemType Directory -Force $localRepo | Out-Null
robocopy $offlineRepo $localRepo /E | Out-Null

$settingsXml = @"
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <offline>true</offline>
</settings>
"@

Set-Content (Join-Path $localM2 "settings.xml") $settingsXml -Encoding UTF8

Write-Host ""
Write-Host "KLART."
Write-Host "1. Starta om Eclipse"
Write-Host "2. Importera projektmappen som Existing Maven Project"
Write-Host "3. Om gammal Eclipse klagar på JavaScript, ignorera JS-validatorn eller stäng av den"
