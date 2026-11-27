package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.Document;
import testsupport.FakeDocumentLibraryController;
import testsupport.FakeDocumentLibraryController.ExportRequest;
import testsupport.TestDocuments;

/**
 * Tests for {@code DownloadWorkflow}. Owner: Member D.
 *
 * <p>Cases D-21..D-24 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DownloadWorkflowTest {

    @TempDir
    Path tempDir;

    private FakeDocumentLibraryController controller;

    private ScriptedDialogService dialogs;

    private DownloadWorkflow workflow;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        controller = new FakeDocumentLibraryController();
        dialogs = new ScriptedDialogService();
        workflow = new DownloadWorkflow(controller, dialogs);
    }

    /**
     * D-21: cancel at save dialog [unit].
     *
     * <p>Arrange: no target.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns false; no export request.</p>
     */
    @Test
    @DisplayName("D-21 cancel at save dialog")
    void run_chooserCancelled_exportsNothing() {
        // Arrange
        dialogs.answerDownloadTarget(Optional.empty());

        // Act
        boolean started = workflow.run(TestDocuments.document("Notes"));

        // Assert
        assertFalse(started);
        assertEquals(List.of(), controller.exportRequests());
    }

    /**
     * D-22: download (UC5, D8) [unit].
     *
     * <p>Arrange: document 'Notes'; target tempDir/Notes.txt (absent).</p>
     * <p>Act: run.</p>
     * <p>Assert: chooser suggested 'Notes.txt'; export request (document, target).</p>
     */
    @Test
    @DisplayName("D-22 download (UC5, D8)")
    void run_newTarget_exportsSnapshotWithDefaultName() {
        // Arrange
        Document document = TestDocuments.document("Notes", "line 1\r\nline 2");
        Path target = tempDir.resolve("Notes.txt");
        dialogs.answerDownloadTarget(Optional.of(target));

        // Act
        boolean started = workflow.run(document);

        // Assert
        assertTrue(started);
        assertEquals(List.of("Notes.txt"), dialogs.questionsOf("chooseDownloadTarget").get(0).arguments());
        assertEquals(List.of(), dialogs.questionsOf("confirmOverwriteFile"));
        assertEquals(List.of(new ExportRequest(document, target)), controller.exportRequests());
    }

    /**
     * D-23: overwrite declined (D8) [unit].
     *
     * <p>Arrange: existing target; confirm = false.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns false; no export request.</p>
     */
    @Test
    @DisplayName("D-23 overwrite declined (D8)")
    void run_existingTargetDeclined_exportsNothing() throws IOException {
        // Arrange
        Path target = TestDocuments.writeAsciiFile(tempDir, "Notes.txt", "old");
        dialogs.answerDownloadTarget(Optional.of(target)).answerConfirmOverwriteFile(false);

        // Act
        boolean started = workflow.run(TestDocuments.document("Notes"));

        // Assert
        assertFalse(started);
        assertEquals(1, dialogs.questionsOf("confirmOverwriteFile").size());
        assertEquals(List.of(), controller.exportRequests());
    }

    /**
     * D-24: overwrite confirmed (D8) [unit].
     *
     * <p>Arrange: existing target; confirm = true.</p>
     * <p>Act: run.</p>
     * <p>Assert: export request present.</p>
     */
    @Test
    @DisplayName("D-24 overwrite confirmed (D8)")
    void run_existingTargetConfirmed_exports() throws IOException {
        // Arrange
        Document document = TestDocuments.document("Notes");
        Path target = TestDocuments.writeAsciiFile(tempDir, "Notes.txt", "old");
        dialogs.answerDownloadTarget(Optional.of(target)).answerConfirmOverwriteFile(true);

        // Act
        boolean started = workflow.run(document);

        // Assert
        assertTrue(started);
        assertEquals(List.of(new ExportRequest(document, target)), controller.exportRequests());
    }
}
