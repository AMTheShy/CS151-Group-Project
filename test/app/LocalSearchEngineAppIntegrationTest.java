package app;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testsupport.ImmediateUiDispatcher;

/**
 * Tests for {@code LocalSearchEngineApp.wire (real A + B + C)}. Owner: Member C.
 *
 * <p>Cases I-01..I-13 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class LocalSearchEngineAppIntegrationTest {

    @TempDir
    Path tempDir;

    private LocalSearchEngineApp.Components components;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        components = LocalSearchEngineApp.wire(tempDir.resolve("documents"), new ImmediateUiDispatcher());
    }

    /**
     * I-01: UC1 end to end [integration].
     *
     * <p>Arrange: sample-small.txt copied to tempDir; importDocument; await COMPLETED.</p>
     * <p>Act: import.</p>
     * <p>Assert: currentDocuments contains it; one read-only .txt in documents dir; content identical.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-01 UC1 end to end")
    void import_realFile_appearsInLibraryWithReadOnlyCopy() {
        fail("TODO: Member C - I-01");
    }

    /**
     * I-02: D3 end to end [integration].
     *
     * <p>Arrange: non-ascii.txt.</p>
     * <p>Act: import.</p>
     * <p>Assert: FAILED + onImportFailed message mentions non-ASCII; nothing stored.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-02 D3 end to end")
    void import_nonAsciiFile_failsWithMessage() {
        fail("TODO: Member C - I-02");
    }

    /**
     * I-03: D2 race end to end [integration].
     *
     * <p>Arrange: two imports of different files with title 'Same', replace=false, started back to
     * back.</p>
     * <p>Act: import.</p>
     * <p>Assert: one COMPLETED, one FAILED 'already exists'.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-03 D2 race end to end")
    void import_sameTitleConcurrently_exactlyOneSucceeds() {
        fail("TODO: Member C - I-03");
    }

    /**
     * I-04: UC2 end to end [integration].
     *
     * <p>Arrange: import s1-match-cases.txt and sample-small.txt; search 'Computer Algorithm'.</p>
     * <p>Act: search.</p>
     * <p>Assert: 2 results sorted by title; match counts 5 and 2; snippets contain the match.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-04 UC2 end to end")
    void search_s1Documents_findsPositivesSortedWithSnippets() {
        fail("TODO: Member C - I-04");
    }

    /**
     * I-05: D5 end to end [integration].
     *
     * <p>Arrange: import 2 docs; shutdown; wire again on same dir; loadPersistedDocuments.</p>
     * <p>Act: startup load.</p>
     * <p>Assert: onStartupLoadCompleted(2, 0); currentDocuments has both.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-05 D5 end to end")
    void restart_sameDirectory_loadsPersistedDocuments() {
        fail("TODO: Member C - I-05");
    }

    /**
     * I-06: UC3 end to end [integration].
     *
     * <p>Arrange: import A, B; deleteDocuments([A]); search.</p>
     * <p>Act: delete.</p>
     * <p>Assert: A absent from list, disk and results.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-06 UC3 end to end")
    void delete_document_removedFromMemoryDiskAndSearch() {
        fail("TODO: Member C - I-06");
    }

    /**
     * I-07: D8 snapshot download [integration].
     *
     * <p>Arrange: import A; take snapshot from search result; delete A; export snapshot.</p>
     * <p>Act: export.</p>
     * <p>Assert: target bytes equal original content.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-07 D8 snapshot download")
    void export_afterDelete_writesSnapshotExactly() {
        fail("TODO: Member C - I-07");
    }

    /**
     * I-08: U3 end to end [integration].
     *
     * <p>Arrange: import 20 x 1 MB docs; startSearch; cancel immediately.</p>
     * <p>Act: search cancel.</p>
     * <p>Assert: no callback within 2 s; handle isDone.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-08 U3 end to end")
    void cancelSearch_largeCorpus_noCallback() {
        fail("TODO: Member C - I-08");
    }

    /**
     * I-09: S5 end to end [integration].
     *
     * <p>Arrange: import blocked large doc + start search on existing docs.</p>
     * <p>Act: search during import.</p>
     * <p>Assert: results exclude the in-flight document.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-09 S5 end to end")
    void searchDuringImport_newDocumentNotInResults() {
        fail("TODO: Member C - I-09");
    }

    /**
     * I-12: R8 end to end [integration].
     *
     * <p>Arrange: run imports and searches; shutdown.</p>
     * <p>Act: shutdown.</p>
     * <p>Assert: no live thread named lse-* after SHUTDOWN_GRACE_MILLIS.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-12 R8 end to end")
    void shutdown_afterWork_allWorkerThreadsTerminate() {
        fail("TODO: Member C - I-12");
    }

    /**
     * I-13: R6 end to end [integration].
     *
     * <p>Arrange: persist 'A' v1 (first run); second run: import 'A' v2 with replace=true while startup
     * load runs.</p>
     * <p>Act: startup load + import.</p>
     * <p>Assert: final 'A' is v2; LoadSummary skipped or loaded consistently; no exception.</p>
     */
    @Test
    @Tag("integration")
    @Disabled("TODO: Member C")
    @DisplayName("I-13 R6 end to end")
    void startupLoadConcurrentWithImport_userDocumentWins() {
        fail("TODO: Member C - I-13");
    }
}
