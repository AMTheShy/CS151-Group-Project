package models.storage;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@code FileSystemDocumentExporter}. Owner: Member A.
 *
 * <p>Cases A-40..A-44 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class FileSystemDocumentExporterTest {

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
    @Disabled("TODO: Member A")
    @DisplayName("A-40 download writes exact content (UC5)")
    void export_newTarget_writesExactBytes() {
        fail("TODO: Member A - A-40");
    }

    /**
     * A-41: overwrite confirmed by the view (D8) [unit].
     *
     * <p>Arrange: target exists with other content.</p>
     * <p>Act: export.</p>
     * <p>Assert: target replaced; no .tmp left.</p>
     */
    @Test
    @Disabled("TODO: Member A")
    @DisplayName("A-41 overwrite confirmed by the view (D8)")
    void export_existingTarget_overwritesIt() {
        fail("TODO: Member A - A-41");
    }

    /**
     * A-42: invalid target folder [unit].
     *
     * <p>Arrange: target tempDir/missing/out.txt.</p>
     * <p>Act: export.</p>
     * <p>Assert: DocumentStorageException mentioning the folder.</p>
     */
    @Test
    @Disabled("TODO: Member A")
    @DisplayName("A-42 invalid target folder")
    void export_missingParentDirectory_throwsDocumentStorageException() {
        fail("TODO: Member A - A-42");
    }

    /**
     * A-43: failure message for the error dialog (UC5) [unit].
     *
     * <p>Arrange: existing target marked read-only.</p>
     * <p>Act: export.</p>
     * <p>Assert: DocumentStorageException; message names target; original content untouched.</p>
     */
    @Test
    @Disabled("TODO: Member A")
    @DisplayName("A-43 failure message for the error dialog (UC5)")
    void export_readOnlyTarget_throwsDocumentStorageExceptionWithMessage() {
        fail("TODO: Member A - A-43");
    }

    /**
     * A-44: cancellation leaves no garbage [unit].
     *
     * <p>Arrange: large document; pre-cancelled token.</p>
     * <p>Act: export.</p>
     * <p>Assert: OperationCancelledException; target absent; no .tmp files.</p>
     */
    @Test
    @Disabled("TODO: Member A")
    @DisplayName("A-44 cancellation leaves no garbage")
    void export_cancelled_leavesNoPartialFile() {
        fail("TODO: Member A - A-44");
    }
}
