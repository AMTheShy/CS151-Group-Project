package models.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;
import models.OperationCancelledException;
import models.SimpleCancellationToken;
import testsupport.TestDocuments;

/**
 * Tests for {@code FileSystemDocumentExporter}. Owner: Member A.
 *
 * <p>Cases A-40..A-44 of {@code design/test-plan.md}.</p>
 */
class FileSystemDocumentExporterTest {

    /** Size of the "large" document in A-44: several write buffers. */
    private static final int LARGE_DOCUMENT_LENGTH = 1024 * 1024;

    /** Seed for generated text, so failures are reproducible. */
    private static final long SEED = 7L;

    @TempDir
    Path tempDir;

    private FileSystemDocumentExporter exporter;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        exporter = new FileSystemDocumentExporter();
    }

    /**
     * A-40: download writes exact content (UC5) [unit].
     *
     * <p>Arrange: document with CRLF content; target tempDir/out.txt.</p>
     * <p>Act: export.</p>
     * <p>Assert: file bytes equal content US-ASCII bytes.</p>
     */
    @Test
    @DisplayName("A-40 download writes exact content (UC5)")
    void export_newTarget_writesExactBytes() throws IOException, DocumentStorageException {
        // Arrange
        Document document = TestDocuments.document("Notes", "line one\r\nline two\nline three\r");
        Path target = tempDir.resolve("out.txt");

        // Act
        exporter.export(document, target, CancellationToken.NONE);

        // Assert
        assertArrayEquals(document.content().getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(target));
        assertEquals(List.of(), temporaryFiles());
    }

    /**
     * A-41: overwrite confirmed by the view (D8) [unit].
     *
     * <p>Arrange: target exists with other content.</p>
     * <p>Act: export.</p>
     * <p>Assert: target replaced; no .tmp left.</p>
     */
    @Test
    @DisplayName("A-41 overwrite confirmed by the view (D8)")
    void export_existingTarget_overwritesIt() throws IOException, DocumentStorageException {
        // Arrange
        Path target = TestDocuments.writeAsciiFile(tempDir, "out.txt", "old content that is longer than the new one");
        Document document = TestDocuments.document("Notes", "new content");

        // Act
        exporter.export(document, target, CancellationToken.NONE);

        // Assert
        assertEquals("new content", Files.readString(target, StandardCharsets.US_ASCII));
        assertEquals(List.of(), temporaryFiles());
    }

    /**
     * A-42: invalid target folder [unit].
     *
     * <p>Arrange: target tempDir/missing/out.txt.</p>
     * <p>Act: export.</p>
     * <p>Assert: DocumentStorageException mentioning the folder.</p>
     */
    @Test
    @DisplayName("A-42 invalid target folder")
    void export_missingParentDirectory_throwsDocumentStorageException() {
        // Arrange
        Path missingFolder = tempDir.resolve("missing");
        Path target = missingFolder.resolve("out.txt");
        Document document = TestDocuments.document("Notes", "content");

        // Act
        DocumentStorageException thrown = assertThrows(DocumentStorageException.class,
                () -> exporter.export(document, target, CancellationToken.NONE));

        // Assert
        assertTrue(thrown.getMessage().contains(missingFolder.toString()),
                "message must name the folder: " + thrown.getMessage());
        assertFalse(Files.exists(missingFolder), "the folder must not be created");
    }

    /**
     * A-43: failure message for the error dialog (UC5) [unit].
     *
     * <p>Arrange: existing target marked read-only.</p>
     * <p>Act: export.</p>
     * <p>Assert: DocumentStorageException; message names target; original content untouched.</p>
     */
    @Test
    @DisplayName("A-43 failure message for the error dialog (UC5)")
    void export_readOnlyTarget_throwsDocumentStorageExceptionWithMessage() throws IOException {
        // Arrange
        Path target = TestDocuments.writeAsciiFile(tempDir, "locked.txt", "original");
        assertTrue(target.toFile().setWritable(false), "could not mark the target read-only");
        try {
            Assumptions.assumeFalse(Files.isWritable(target), "running with privileges that ignore read-only flags");
            Document document = TestDocuments.document("Notes", "replacement");

            // Act
            DocumentStorageException thrown = assertThrows(DocumentStorageException.class,
                    () -> exporter.export(document, target, CancellationToken.NONE));

            // Assert
            assertTrue(thrown.getMessage().contains(target.toString()),
                    "message must name the target: " + thrown.getMessage());
            assertTrue(thrown.getMessage().contains("access denied"),
                    "message must state the cause: " + thrown.getMessage());
            assertEquals("original", Files.readString(target, StandardCharsets.US_ASCII));
            assertEquals(List.of(), temporaryFiles());
        } finally {
            target.toFile().setWritable(true);
        }
    }

    /**
     * A-44: cancellation leaves no garbage [unit].
     *
     * <p>Arrange: large document; pre-cancelled token.</p>
     * <p>Act: export.</p>
     * <p>Assert: OperationCancelledException; target absent; no .tmp files.</p>
     */
    @Test
    @DisplayName("A-44 cancellation leaves no garbage")
    void export_cancelled_leavesNoPartialFile() throws IOException {
        // Arrange
        Document document = TestDocuments.document("Large", TestDocuments.asciiText(LARGE_DOCUMENT_LENGTH, SEED));
        Path target = tempDir.resolve("large.txt");
        SimpleCancellationToken token = new SimpleCancellationToken();
        token.cancel();

        // Act
        assertThrows(OperationCancelledException.class, () -> exporter.export(document, target, token));

        // Assert
        assertFalse(Files.exists(target), "no partial target may be left behind");
        assertEquals(List.of(), temporaryFiles());
    }

    /**
     * Lists leftover temporary files in the test directory.
     *
     * @return the names of all {@code .tmp} files, empty if none
     * @throws IOException if listing fails
     */
    private List<String> temporaryFiles() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".tmp"))
                    .toList();
        }
    }
}
