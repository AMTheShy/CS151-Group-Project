$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $version = '1.10.2'
    $junit = $env:JUNIT_JAR
    if (-not $junit) {
        $junit = "lib/junit-platform-console-standalone-$version.jar"
        if (-not (Test-Path $junit)) {
            New-Item -ItemType Directory -Path lib -Force | Out-Null
            $temp = New-TemporaryFile
            try {
                $url = "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/$version/junit-platform-console-standalone-$version.jar"
                Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $temp.FullName
                Move-Item $temp.FullName $junit -Force
            } finally {
                if (Test-Path $temp.FullName) {
                    Remove-Item $temp.FullName
                }
            }
        }
    }
    if (-not (Test-Path $junit -PathType Leaf)) {
        throw "JUnit JAR not found: $junit"
    }

    & "$PSScriptRoot/compile.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    New-Item -ItemType Directory -Path out/test -Force | Out-Null
    $sources = Get-ChildItem test -Recurse -Filter '*.java' | ForEach-Object {
        '"' + ($_.FullName -replace '\\', '/') + '"'
    }
    [IO.File]::WriteAllLines((Join-Path (Get-Location).Path "out/test-sources.txt"),
        [string[]]$sources, [Text.UTF8Encoding]::new($false))

    $separator = [IO.Path]::PathSeparator
    $compileClasspath = @('out/main', $junit) -join $separator
    & javac --release 17 -cp $compileClasspath -d out/test '@out/test-sources.txt'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $options = $args
    if ($options.Count -eq 0) {
        $options = @('--scan-class-path', '--exclude-tag', 'benchmark')
    }

    $testClasspath = @('out/main', 'out/test') -join $separator
    & java '-Djava.awt.headless=true' -jar $junit execute --class-path $testClasspath --fail-if-no-tests @options
    exit $LASTEXITCODE
} finally {
    Pop-Location
}