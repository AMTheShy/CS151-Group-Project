# Run and test Local Search Engine

Install JDK 17 or newer and make sure both `java` and `javac` are on your PATH.
Run these commands from the repository root. The application needs a graphical
desktop. Tests run headless.

| Task | macOS or Linux | Windows PowerShell |
|---|---|---|
| Start the application | `bash build/run.sh` | `.\build\run.ps1` |
| Start with a 1 GB heap | `bash build/run.sh -Xmx1g` | `.\build\run.ps1 -Xmx1g` |
| Run all tests except benchmarks | `bash build/test.sh` | `.\build\test.ps1` |
| Run Member C controller tests | `bash build/test.sh --select-package controllers` | `.\build\test.ps1 --select-package controllers` |
| Run the integration tests | `bash build/test.sh --scan-class-path --include-tag integration` | `.\build\test.ps1 --scan-class-path --include-tag integration` |
| Run benchmarks | `bash build/test.sh --scan-class-path --include-tag benchmark` | `.\build\test.ps1 --scan-class-path --include-tag benchmark` |

The test scripts download JUnit Platform Console Standalone 1.10.2 from Maven
Central to `lib/` on the first run. For offline testing, place that JAR in
`lib/` with its original filename or set `JUNIT_JAR` to its absolute path.
Test classes are compiled into `out/test` and production classes into `out/main`.

The application stores imports under `~/.local-search-engine/documents`.
Integration tests use temporary directories.

To try the GUI, import `test/resources/sample-small.txt`, search for
`Computer Algorithm`, open the result, verify both matches and navigation,
download a copy, and delete the document through the library.