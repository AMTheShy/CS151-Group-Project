# Test resources

Committed test data. Large files (5 MB, 20 MB, 100 MB) are generated at run time by the
benchmark tests with `testsupport.TestDocuments.asciiText` and are never committed.

| File | Content | Used for |
|---|---|---|
| `sample-small.txt` | 4 LF-terminated lines, 2 matches of "computer algorithm" | smoke / integration tests |
| `s1-match-cases.txt` | the 5 S1 positive examples, a `---` line, then the S1 negative examples (incl. tab, LF and CRLF between the words) | S1 tests; the whole file contains exactly **5** matches of "Computer Algorithm" |
| `non-ascii.txt` | ASCII text with byte `0xE9` at offset 18 | D3 rejection (`TestDocuments.NON_ASCII_OFFSET`) |
| `empty.txt` | 0 bytes | empty file is valid (D3) |
| `mixed-line-breaks.txt` | `line one\r\nline two\nline three\rline four` (no final break) | line breaks preserved verbatim (D3), CR display (N9) |
| `snippet-boundaries.txt` | 100 `x`, `NEEDLE`, 100 `y` (206 bytes) | snippet truncation (S4) |
| `overlap-cases.txt` | `aaaa` | non-overlapping matches (S3): query `aa` gives 2 matches |
