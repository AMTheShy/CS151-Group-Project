package models.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.CancellationToken;
import models.DocumentException;
import models.InvalidDocumentException;
import models.OperationCancelledException;
import models.ProgressListener;
import models.SimpleCancellationToken;
import testsupport.TestDocuments;

/**
 * Tests for {@code StreamingDocumentLoader}. Owner: Member A.
 *
 * <p>Cases A-31..A-39 of {@code design/test-plan.md}.</p>
 */
class StreamingDocumentLoaderTest {

    /** Size limit used by the limit tests A-34 and A-35. */
    private static final int SMALL_LIMIT = 1000;

    /** One kilobyte, for readable file sizes. */
    private static final int KILOBYTE = 1024;

    /** One megabyte, for readable file sizes. */
    private static final int MEGABYTE = 1024 * 1024;

    /** Seed for generated text, so failures are reproducible. */
    private static final long SEED = 42L;

    /** Warm-up runs before measuring (CONVENTIONS Q8). */
    private static final int WARM_UP_RUNS = 3;

    /** Measured runs; the median is asserted (CONVENTIONS Q8). */
    private static final int MEASURED_RUNS = 5;

    /** A-39 budget for loading 5 MB, in milliseconds (test plan section 4). */
    private static final long LOAD_BUDGET_MILLIS = 300;

    @TempDir
    Path tempDir;

    private StreamingDocumentLoader loader;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        loader = new StreamingDocumentLoader();
    }

    /**
     * A-31: line breaks preserved verbatim (D3) [unit].
     *
     * <p>Arrange: resource mixed-line-breaks.txt.</p>
     * <p>Act: load.</p>
     * <p>Assert: returns exactly the file bytes as String (CRLF, LF, CR kept).</p>
     */
    @Test
    @DisplayName("A-31 line breaks preserved verbatim (D3)")
    void load_asciiFileWithMixedLineBreaks_returnsExactContent() throws IOException, DocumentException {
        // Arrange
        Path source = TestDocuments.resource(TestDocuments.RESOURCE_MIXED_LINE_BREAKS);
        byte[] expectedBytes = Files.readAllBytes(source);

        // Act
        String content = loader.load(source, ProgressListener.NONE, CancellationToken.NONE);

        // Assert
        assertArrayEquals(expectedBytes, content.getBytes(StandardCharsets.US_ASCII));
        assertTrue(content.contains("one\r\nline"), "CRLF must be kept");
        assertTrue(content.contains("two\nline"), "LF must be kept");
        assertTrue(content.contains("three\rline"), "CR must be kept");
    }

    /**
     * A-32: empty file allowed (D3) [unit].
     *
     * <p>Arrange: resource empty.txt.</p>
     * <p>Act: load.</p>
     * <p>Assert: returns ""; progress (0,0) reported.</p>
     */
    @Test
    @DisplayName("A-32 empty file allowed (D3)")
    void load_emptyFile_returnsEmptyString() throws DocumentException {
        // Arrange
        Path source = TestDocuments.resource(TestDocuments.RESOURCE_EMPTY);
        List<long[]> progressCalls = new ArrayList<>();

        // Act
        String content = loader.load(source, recordingListener(progressCalls), CancellationToken.NONE);

        // Assert
        assertEquals("", content);
        assertEquals(1, progressCalls.size());
        assertArrayEquals(new long[] {0, 0}, progressCalls.get(0));
    }

    /**
     * A-33: non-ASCII rejected (D3) [unit].
     *
     * <p>Arrange: resource non-ascii.txt (0xE9 at offset 18).</p>
     * <p>Act: load.</p>
     * <p>Assert: InvalidDocumentException NON_ASCII_CONTENT; message contains '18'.</p>
     */
    @Test
    @DisplayName("A-33 non-ASCII rejected (D3)")
    void load_nonAsciiByte_throwsInvalidDocumentExceptionWithOffset() {
        // Arrange
        Path source = TestDocuments.resource(TestDocuments.RESOURCE_NON_ASCII);

        // Act
        InvalidDocumentException thrown = assertThrows(InvalidDocumentException.class,
                () -> loader.load(source, ProgressListener.NONE, CancellationToken.NONE));

        // Assert
        assertEquals(InvalidDocumentException.Reason.NON_ASCII_CONTENT, thrown.reason());
        assertTrue(thrown.getMessage().contains(String.valueOf(TestDocuments.NON_ASCII_OFFSET)),
                "message must name the offset: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("0xE9"), "message must name the byte: " + thrown.getMessage());
    }

    /**
     * A-34: size limit (D3) [unit].
     *
     * <p>Arrange: loader with limit 1000; 1001-byte file; progress listener records calls.</p>
     * <p>Act: load.</p>
     * <p>Assert: FILE_TOO_LARGE; progress listener never called.</p>
     */
    @Test
    @DisplayName("A-34 size limit (D3)")
    void load_fileLargerThanLimit_throwsFileTooLargeWithoutReading() throws IOException {
        // Arrange
        StreamingDocumentLoader limitedLoader = new StreamingDocumentLoader(SMALL_LIMIT);
        Path source = TestDocuments.writeAsciiFile(tempDir, "too-large.txt",
                TestDocuments.asciiText(SMALL_LIMIT + 1, SEED));
        List<long[]> progressCalls = new ArrayList<>();

        // Act
        InvalidDocumentException thrown = assertThrows(InvalidDocumentException.class,
                () -> limitedLoader.load(source, recordingListener(progressCalls), CancellationToken.NONE));

        // Assert
        assertEquals(InvalidDocumentException.Reason.FILE_TOO_LARGE, thrown.reason());
        assertTrue(progressCalls.isEmpty(), "the content must not be read before the size check");
    }

    /**
     * A-35: size limit is inclusive [unit].
     *
     * <p>Arrange: loader with limit 1000; 1000-byte ASCII file.</p>
     * <p>Act: load.</p>
     * <p>Assert: returns 1000 characters.</p>
     */
    @Test
    @DisplayName("A-35 size limit is inclusive")
    void load_fileExactlyAtLimit_isAccepted() throws IOException, DocumentException {
        // Arrange
        StreamingDocumentLoader limitedLoader = new StreamingDocumentLoader(SMALL_LIMIT);
        String expected = TestDocuments.asciiText(SMALL_LIMIT, SEED);
        Path source = TestDocuments.writeAsciiFile(tempDir, "at-limit.txt", expected);

        // Act
        String content = limitedLoader.load(source, ProgressListener.NONE, CancellationToken.NONE);

        // Assert
        assertEquals(SMALL_LIMIT, content.length());
        assertEquals(expected, content);
    }

    /**
     * A-36: not a regular file [unit].
     *
     * <p>Arrange: tempDir itself; tempDir/missing.txt.</p>
     * <p>Act: load.</p>
     * <p>Assert: InvalidDocumentException NOT_A_REGULAR_FILE for both.</p>
     */
    @Test
    @DisplayName("A-36 not a regular file")
    void load_directoryOrMissingPath_throwsNotARegularFile() {
        // Arrange
        Path directory = tempDir;
        Path missing = tempDir.resolve("missing.txt");

        // Act
        InvalidDocumentException directoryFailure = assertThrows(InvalidDocumentException.class,
                () -> loader.load(directory, ProgressListener.NONE, CancellationToken.NONE));
        InvalidDocumentException missingFailure = assertThrows(InvalidDocumentException.class,
                () -> loader.load(missing, ProgressListener.NONE, CancellationToken.NONE));

        // Assert
        assertEquals(InvalidDocumentException.Reason.NOT_A_REGULAR_FILE, directoryFailure.reason());
        assertEquals(InvalidDocumentException.Reason.NOT_A_REGULAR_FILE, missingFailure.reason());
        assertTrue(missingFailure.getMessage().contains("missing.txt"),
                "message must name the file: " + missingFailure.getMessage());
    }

    /**
     * A-37: byte progress (U2) [unit].
     *
     * <p>Arrange: 300 KB file.</p>
     * <p>Act: load.</p>
     * <p>Assert: progress calls non-decreasing, last = (total,total), at least 4 calls.</p>
     */
    @Test
    @DisplayName("A-37 byte progress (U2)")
    void load_multiBufferFile_reportsMonotonicProgressEndingAtTotal() throws IOException, DocumentException {
        // Arrange
        int total = 300 * KILOBYTE;
        Path source = TestDocuments.writeAsciiFile(tempDir, "progress.txt", TestDocuments.asciiText(total, SEED));
        List<long[]> progressCalls = new ArrayList<>();

        // Act
        loader.load(source, recordingListener(progressCalls), CancellationToken.NONE);

        // Assert
        assertTrue(progressCalls.size() >= 4, "expected at least 4 progress calls, got " + progressCalls.size());
        long previousCompleted = 0;
        for (long[] call : progressCalls) {
            assertTrue(call[0] >= previousCompleted, "completed must not decrease: " + Arrays.toString(call));
            assertTrue(call[0] <= call[1], "completed must not exceed total: " + Arrays.toString(call));
            previousCompleted = call[0];
        }
        assertArrayEquals(new long[] {total, total}, progressCalls.get(progressCalls.size() - 1));
    }

    /**
     * A-38: cancellation [unit].
     *
     * <p>Arrange: 1 MB file; token cancelled inside first progress callback.</p>
     * <p>Act: load.</p>
     * <p>Assert: OperationCancelledException.</p>
     */
    @Test
    @DisplayName("A-38 cancellation")
    void load_cancelledMidRead_throwsOperationCancelledException() throws IOException {
        // Arrange
        Path source = TestDocuments.writeAsciiFile(tempDir, "cancel.txt", TestDocuments.asciiText(MEGABYTE, SEED));
        SimpleCancellationToken token = new SimpleCancellationToken();
        List<long[]> progressCalls = new ArrayList<>();
        ProgressListener cancelOnFirstCall = (completed, total) -> {
            progressCalls.add(new long[] {completed, total});
            token.cancel();
        };

        // Act / Assert
        assertThrows(OperationCancelledException.class, () -> loader.load(source, cancelOnFirstCall, token));
        assertEquals(1, progressCalls.size(), "reading must stop at the next buffer after cancellation");
    }

    /**
     * A-39: import performance (3.4) [benchmark].
     *
     * <p>Arrange: 5 MB generated ASCII file; 3 warm-up runs.</p>
     * <p>Act: load.</p>
     * <p>Assert: median load time &lt;= 300 ms (contributes to 1 s import budget).</p>
     */
    @Test
    @Tag("benchmark")
    @DisplayName("A-39 import performance (3.4)")
    void load_fiveMegabyteFile_completesWithinBudget() throws IOException, DocumentException {
        // Arrange
        int size = 5 * MEGABYTE;
        Path source = TestDocuments.writeAsciiFile(tempDir, "five-mb.txt", TestDocuments.asciiText(size, SEED));
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            loader.load(source, ProgressListener.NONE, CancellationToken.NONE);
        }

        // Act
        long[] durationsNanos = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            String content = loader.load(source, ProgressListener.NONE, CancellationToken.NONE);
            durationsNanos[i] = System.nanoTime() - start;
            assertEquals(size, content.length());
        }

        // Assert
        Arrays.sort(durationsNanos);
        long medianMillis = durationsNanos[MEASURED_RUNS / 2] / 1_000_000;
        assertTrue(medianMillis <= LOAD_BUDGET_MILLIS,
                "median load time " + medianMillis + " ms exceeds " + LOAD_BUDGET_MILLIS + " ms");
    }

    /**
     * Creates a listener that records every call as {@code {completed, total}}.
     *
     * @param calls receives the calls
     * @return the listener
     */
    private static ProgressListener recordingListener(List<long[]> calls) {
        return (completed, total) -> calls.add(new long[] {completed, total});
    }
}
