#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p out/main
find main -name '*.java' | sort > out/main-sources.txt
javac --release 17 -d out/main @out/main-sources.txt