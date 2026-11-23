package app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.ImportState;
import models.SearchQuery;
import testsupport.ImmediateUiDispatcher;
import testsupport.RecordingLibraryListener;
import testsupport.RecordingSearchListener;
import testsupport.TestDocuments;

@Tag("benchmark")
class EndToEndBenchmarkTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @TempDir
    Path tempDir;

    private LocalSearchEngineApp.Components components;

    @BeforeEach
    void setUp() {
        components = LocalSearchEngineApp.wire(
                tempDir.resolve("documents"), new ImmediateUiDispatcher());
    }

    @AfterEach
    void shutDown() {
        if (components != null) {
            components.lifecycle().shutdown();
        }
    }

    @Test
    @DisplayName("I-10 100 MB search <= 1 s (3.4)")
    void search_hundredMegabyteCorpus_completesWithinOneSecond() throws Exception {
        Path source = TestDocuments.writeAsciiFile(
                tempDir, "corpus.txt", "a".repeat(5 * 1024 * 1024));

        for (int i = 0; i < 20; i++) {
            importAndMeasure(source, "Doc " + i);
        }

        measureSearch();
        measureSearch();

        long[] elapsed = {measureSearch(), measureSearch(), measureSearch()};
        Arrays.sort(elapsed);

        assertTrue(elapsed[1] <= TimeUnit.SECONDS.toNanos(1),
                "Median: " + TimeUnit.NANOSECONDS.toMillis(elapsed[1]) + " ms");
    }

    @Test
    @DisplayName("I-11 5 MB import <= 1 s (3.4)")
    void import_fiveMegabyteFile_completesWithinOneSecond() throws Exception {
        Path source = TestDocuments.writeAsciiFile(
                tempDir, "large.txt", "a".repeat(5 * 1024 * 1024));

        long elapsed = importAndMeasure(source, "Large");

        assertTrue(elapsed <= TimeUnit.SECONDS.toNanos(1),
                "Import: " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms");
    }

    private long importAndMeasure(Path source, String title) throws InterruptedException {
        var events = new RecordingLibraryListener();
        components.library().addLibraryListener(events);
        try {
            long start = System.nanoTime();
            long id = components.library().importDocument(source, title, false);

            assertTrue(events.await(listener -> listener.isTerminal(id), TIMEOUT));
            long elapsed = System.nanoTime() - start;

            var progress = events.progressOf(id);
            assertEquals(ImportState.COMPLETED, progress.get(progress.size() - 1).state());
            return elapsed;
        } finally {
            components.library().removeLibraryListener(events);
        }
    }

    private long measureSearch() throws InterruptedException {
        var results = new RecordingSearchListener();
        long start = System.nanoTime();

        components.search().startSearch(SearchQuery.of("computer algorithm"), results);
        assertTrue(results.awaitCallback(TIMEOUT));

        long elapsed = System.nanoTime() - start;
        assertTrue(results.failures().isEmpty());
        assertEquals(1, results.completions().size());
        return elapsed;
    }
}