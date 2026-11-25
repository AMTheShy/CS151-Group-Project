#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
bash build/compile.sh
exec java "$@" -cp out/main app.LocalSearchEngineApp