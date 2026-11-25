$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    & "$PSScriptRoot/compile.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    & java @args -cp out/main app.LocalSearchEngineApp
    exit $LASTEXITCODE
} finally {
    Pop-Location
}