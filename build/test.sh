#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

version=1.10.2
junit="${JUNIT_JAR:-lib/junit-platform-console-standalone-$version.jar}"

if [[ ! -s "$junit" ]]; then
    if [[ -n "${JUNIT_JAR:-}" ]]; then
        echo "JUnit JAR not found: $junit" >&2
        exit 1
    fi

    mkdir -p lib
    temp=$(mktemp lib/junit-download.XXXXXX)
    trap 'rm -f "$temp"' EXIT

    curl --fail --silent --show-error --location \
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/$version/junit-platform-console-standalone-$version.jar" \
        --output "$temp"

    mv "$temp" "$junit"
    trap - EXIT
fi

bash build/compile.sh
mkdir -p out/test
find test -name '*.java' | sort > out/test-sources.txt
javac --release 17 -cp "out/main:$junit" -d out/test @out/test-sources.txt

if [[ $# -eq 0 ]]; then
    set -- --scan-class-path --exclude-tag benchmark
fi

exec java -Djava.awt.headless=true -jar "$junit" execute \
    --class-path "out/main:out/test" --fail-if-no-tests "$@"