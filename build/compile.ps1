$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    New-Item -ItemType Directory -Path out/main -Force | Out-Null
    $sources = Get-ChildItem main -Recurse -Filter '*.java' | ForEach-Object {
        '"' + ($_.FullName -replace '\\', '/') + '"'
    }
    [IO.File]::WriteAllLines((Join-Path (Get-Location).Path "out/main-sources.txt"),
        [string[]]$sources, [Text.UTF8Encoding]::new($false))

    & javac --release 17 -d out/main '@out/main-sources.txt'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}