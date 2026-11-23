package models.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.CancellationToken;
import models.Document;
import models.DocumentException;
import models.ProgressListener;
import testsupport.TestDocuments;

/**
 * Tests for {@code StreamingDocumentLoader + DefaultDocumentRepository}. Owner: Member A.
 *
 * <p>Case A-45 of {@code design/test-plan.md}.</p>
 */
class ImportPipelineBenchmarkTest {

    /** Size of the imported file: 5 MB. */
    private static final int FILE_SIZE = 5 * 1024 * 1024;

    /** Seed for generated text, so failures are reproducible. */
    private static final long SEED = 45L;

    /** Warm-up runs before measuring (CONVENTIONS Q8). */
    private static final int WARM_UP_RUNS = 3;

    /** Measured runs; the median is asserted (CONVENTIONS Q8). */
    private static final int MEASURED_RUNS = 5;

    /** A-45 budget for one import, in milliseconds (design doc 3.4). */
    private static final long IMPORT_BUDGET_MILLIS = 1000;

    @TempDir
    Path tempDir;

    /**
     * A-45: import 5 MB &lt;= 1 s without user interaction (3.4) [benchmark].
     *
     * <p>Arrange: 5 MB file; StreamingDocumentLoader then
     * DefaultDocumentRepository(FileSystemDocumentStore).add.</p>
     * <p>Act: load + add.</p>
     * <p>Assert: total wall time &lt;= 1000 ms (median of 3 after warm-up).</p>
     */
    @Test
    @Tag("benchmark")
    @DisplayName("A-45 import 5 MB <= 1 s without user interaction (3.4)")
    void importPipeline_fiveMegabyteFile_completesWithinOneSecond() throws IOException, DocumentException {
        // Arrange
        Path source = TestDocuments.writeAsciiFile(tempDir, "input.txt", TestDocuments.asciiText(FILE_SIZE, SEED));
        StreamingDocumentLoader loader = new StreamingDocumentLoader();
        DefaultDocumentRepository repository =
                new DefaultDocumentRepository(new FileSystemDocumentStore(tempDir.resolve("documents")));
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            importOnce(loader, repository, source);
        }

        // Act
        long[] durationsNanos = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            importOnce(loader, repository, source);
            durationsNanos[i] = System.nanoTime() - start;
        }

        // Assert
        Arrays.sort(durationsNanos);
        long medianMillis = durationsNanos[MEASURED_RUNS / 2] / 1_000_000;
        assertTrue(medianMillis <= IMPORT_BUDGET_MILLIS,
                "median import time " + medianMillis + " ms exceeds " + IMPORT_BUDGET_MILLIS + " ms");
        assertEquals(1, repository.size());
        assertEquals(FILE_SIZE, Files.size(source));
    }

    /**
     * Runs one full import: read and validate the file, then commit it (replacing the previous run).
     *
     * @param loader     the loader
     * @param repository the repository backed by the real file-system store
     * @param source     the file to import
     * @throws DocumentException if loading or storing fails
     */
    private static void importOnce(StreamingDocumentLoader loader, DefaultDocumentRepository repository, Path source)
            throws DocumentException {
        String content = loader.load(source, ProgressListener.NONE, CancellationToken.NONE);
        repository.add(new Document("Benchmark", content, Instant.now()), true);
    }
}
