$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    python .\tools\bootstrap.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test.ps1
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    python .\tools\build.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
