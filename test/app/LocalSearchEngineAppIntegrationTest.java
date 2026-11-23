package app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controllers.LibraryListener;
import controllers.UiDispatcher;
import models.AppConstants;
import models.Document;
import models.DocumentSearchResult;
import models.DocumentSummary;
import models.ImportProgress;
import models.ImportState;
import models.SearchQuery;
import testsupport.ImmediateUiDispatcher;
import testsupport.QueueingUiDispatcher;
import testsupport.RecordingLibraryListener;
import testsupport.RecordingSearchListener;
import testsupport.TestDocuments;

@Tag("integration")
class LocalSearchEngineAppIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    Path tempDir;

    private LocalSearchEngineApp.Components components;
    private RecordingLibraryListener libraryEvents;
    private final AtomicReference<UiDispatcher> dispatchOverride =
            new AtomicReference<>(new ImmediateUiDispatcher());

    @BeforeEach
    void setUp() {
        restart();
    }

    @AfterEach
    void shutdown() {
        if (components != null) {
            components.lifecycle().shutdown();
        }
    }

    @Test
    @DisplayName("I-01 UC1 end to end")
    void import_realFile_appearsInLibraryWithReadOnlyCopy() throws Exception {
        String original = TestDocuments.readResource("sample-small.txt");
        importText("A", original);

        assertEquals(original,
                components.library().currentDocuments().get(0).document().content());

        List<Path> stored = storedTextFiles();
        assertEquals(1, stored.size());
        Path copy = stored.get(0);
        assertEquals(original, Files.readString(copy));

        var permissions = Files.getFileAttributeView(copy, PosixFileAttributeView.class);
        if (permissions != null) {
            assertFalse(permissions.readAttributes().permissions()
                    .contains(PosixFilePermission.OWNER_WRITE));
        } else {
            var dos = Files.getFileAttributeView(copy, DosFileAttributeView.class);
            if (dos != null) {
                assertTrue(dos.readAttributes().isReadOnly());
            }
        }
    }

    @Test
    @DisplayName("I-02 D3 end to end")
    void import_nonAsciiFile_failsWithMessage() throws Exception {
        long id = components.library().importDocument(
                TestDocuments.resource("non-ascii.txt"), "Invalid", false);

        assertTrue(libraryEvents.await(events -> !events.importFailures().isEmpty(), TIMEOUT));
        var failure = libraryEvents.importFailures().get(0);
        assertEquals(id, failure.importId());
        assertTrue(failure.errorMessage().orElseThrow()
                .toLowerCase(Locale.ROOT).contains("non-ascii"));
        assertTrue(components.library().currentDocuments().isEmpty());
    }

    @Test
    @DisplayName("I-03 D2 race end to end")
    void import_sameTitleConcurrently_exactlyOneSucceeds() throws Exception {
        Path first = TestDocuments.writeAsciiFile(tempDir, "one.txt", "one");
        Path second = TestDocuments.writeAsciiFile(tempDir, "two.txt", "two");

        long a = components.library().importDocument(first, "Same", false);
        long b = components.library().importDocument(second, "Same", false);

        assertTrue(libraryEvents.await(events -> events.isTerminal(a)
                && events.isTerminal(b) && events.importFailures().size() == 1, TIMEOUT));
        assertTrue(libraryEvents.importFailures().get(0).errorMessage()
                .orElseThrow().contains("already exists"));
        assertEquals(1, components.library().currentDocuments().size());
    }

    @Test
    @DisplayName("I-04 UC2 end to end")
    void search_s1Documents_findsPositivesSortedWithSnippets() throws Exception {
        importText("Z sample", TestDocuments.readResource("sample-small.txt"));
        importText("A cases", TestDocuments.readResource("s1-match-cases.txt"));

        var results = search("Computer Algorithm");
        assertEquals(List.of("A cases", "Z sample"),
                results.stream().map(DocumentSearchResult::title).toList());
        assertEquals(List.of(5, 2),
                results.stream().map(DocumentSearchResult::matchCount).toList());
        assertTrue(results.stream().allMatch(result -> result.snippet().text()
                .toLowerCase(Locale.ROOT).contains("computer algorithm")));
    }

    @Test
    @DisplayName("I-05 D5 end to end")
    void restart_sameDirectory_loadsPersistedDocuments() throws Exception {
        importText("A", "one");
        importText("B", "two");

        restart();
        components.library().loadPersistedDocuments();
        awaitStartup();

        var summary = libraryEvents.startupCompletions().get(0);
        assertEquals(2, summary.loadedCount());
        assertEquals(0, summary.skippedCount());
        assertEquals(List.of("A", "B"), components.library().currentDocuments()
                .stream().map(DocumentSummary::title).toList());
    }

    @Test
    @DisplayName("I-06 UC3 end to end")
    void delete_document_removedFromMemoryDiskAndSearch() throws Exception {
        importText("A", "needle one");
        importText("B", "needle two");

        deleteA();

        List<Path> stored = storedTextFiles();
        assertEquals(1, stored.size());
        assertEquals("needle two", Files.readString(stored.get(0)));
        assertEquals(List.of("B"),
                search("needle").stream().map(DocumentSearchResult::title).toList());
    }

    @Test
    @DisplayName("I-07 D8 snapshot download")
    void export_afterDelete_writesSnapshotExactly() throws Exception {
        String text = "needle\r\nsecond line\t";
        importText("A", text);
        Document snapshot = search("needle").get(0).document();
        deleteA();

        Path target = tempDir.resolve("export.txt");
        components.library().exportDocument(snapshot, target);

        assertTrue(libraryEvents.await(events -> !events.exportEvents().isEmpty(), TIMEOUT));
        assertTrue(libraryEvents.exportEvents().get(0).succeeded());
        assertArrayEquals(text.getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(target));
    }

    @Test
    @DisplayName("I-08 U3 end to end")
    void cancelSearch_largeCorpus_noCallback() throws Exception {
        for (int i = 0; i < 20; i++) {
            importText("Doc " + i, "a".repeat(1024 * 1024));
        }

        QueueingUiDispatcher deferred = new QueueingUiDispatcher();
        dispatchOverride.set(deferred);
        RecordingSearchListener results = new RecordingSearchListener();
        var handle = components.search().startSearch(SearchQuery.of("needle"), results);

        handle.cancel();
        deferred.flush();

        assertFalse(results.awaitCallback(Duration.ofSeconds(2)));
        assertTrue(handle.isDone());
        assertTrue(handle.isCancelled());
    }

    @Test
    @DisplayName("I-09 S5 end to end")
    void searchDuringImport_newDocumentNotInResults() throws Exception {
        importText("A", "needle");

        var reading = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        components.library().addLibraryListener(new LibraryListener() {
            @Override
            public void onImportProgress(ImportProgress progress) {
                if (progress.state() == ImportState.READING && progress.title().equals("B")) {
                    reading.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) {
                            throw new AssertionError("Import did not resume");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        Path source = TestDocuments.writeAsciiFile(tempDir, "pending.txt", "needle");
        long id = components.library().importDocument(source, "B", false);

        try {
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("A"),
                    search("needle").stream().map(DocumentSearchResult::title).toList());
        } finally {
            release.countDown();
        }

        assertTrue(libraryEvents.await(events -> events.isTerminal(id), TIMEOUT));
    }

    @Test
    @DisplayName("I-12 R8 end to end")
    void shutdown_afterWork_allWorkerThreadsTerminate() throws Exception {
        importText("A", "needle");
        search("needle");

        List<Thread> workers = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("lse-"))
                .toList();
        assertFalse(workers.isEmpty());

        components.lifecycle().shutdown();

        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(AppConstants.SHUTDOWN_GRACE_MILLIS);
        for (Thread worker : workers) {
            long remaining = deadline - System.nanoTime();
            if (remaining > 0) {
                worker.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
            }
            assertFalse(worker.isAlive(), worker.getName());
        }
    }

    @Test
    @DisplayName("I-13 R6 end to end")
    void startupLoadConcurrentWithImport_userDocumentWins() throws Exception {
        importText("A", "version one");
        restart();

        Path source = TestDocuments.writeAsciiFile(tempDir, "new.txt", "version two");
        components.library().loadPersistedDocuments();
        long id = components.library().importDocument(source, "A", true);

        awaitStartup();
        assertTrue(libraryEvents.await(events -> events.isTerminal(id), TIMEOUT));
        assertTrue(libraryEvents.importFailures().isEmpty());

        var summary = libraryEvents.startupCompletions().get(0);
        assertEquals(1, summary.loadedCount() + summary.skippedCount());
        assertEquals("version two",
                components.library().currentDocuments().get(0).document().content());
    }

    private void restart() {
        shutdown();
        components = LocalSearchEngineApp.wire(tempDir.resolve("documents"),
                task -> dispatchOverride.get().dispatch(task));
        libraryEvents = new RecordingLibraryListener();
        components.library().addLibraryListener(libraryEvents);
    }

    private void importText(String title, String text) throws Exception {
        Path source = Files.createTempFile(tempDir, "fixture-", ".txt");
        Files.writeString(source, text);
        long id = components.library().importDocument(source, title, false);

        assertTrue(libraryEvents.await(events -> events.isTerminal(id), TIMEOUT));

        var progress = libraryEvents.progressOf(id);
        var last = progress.get(progress.size() - 1);
        assertEquals(ImportState.COMPLETED, last.state(), last.errorMessage().orElse(""));
    }

    private List<DocumentSearchResult> search(String query) throws InterruptedException {
        RecordingSearchListener results = new RecordingSearchListener();
        components.search().startSearch(SearchQuery.of(query), results);

        assertTrue(results.awaitCallback(TIMEOUT));
        assertTrue(results.failures().isEmpty(), results.failures().toString());
        return results.completions().get(0);
    }

    private List<Path> storedTextFiles() throws Exception {
        try (var paths = Files.list(tempDir.resolve("documents"))) {
            return paths.filter(path -> path.toString().endsWith(".txt")).toList();
        }
    }

    private void deleteA() throws InterruptedException {
        components.library().deleteDocuments(List.of("A"));
        assertTrue(libraryEvents.await(events -> events.latestDocuments().stream()
                .noneMatch(document -> document.title().equals("A")), TIMEOUT));
    }

    private void awaitStartup() throws InterruptedException {
        assertTrue(libraryEvents.await(events -> !events.startupCompletions().isEmpty(), TIMEOUT));
    }
}