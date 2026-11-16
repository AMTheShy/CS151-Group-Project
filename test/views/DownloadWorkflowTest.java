package views;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testsupport.FakeDocumentLibraryController;

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
    @Disabled("TODO: Member D")
    @DisplayName("D-21 cancel at save dialog")
    void run_chooserCancelled_exportsNothing() {
        fail("TODO: Member D - D-21");
    }

    /**
     * D-22: download (UC5, D8) [unit].
     *
     * <p>Arrange: document 'Notes'; target tempDir/Notes.txt (absent).</p>
     * <p>Act: run.</p>
     * <p>Assert: chooser suggested 'Notes.txt'; export request (document, target).</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-22 download (UC5, D8)")
    void run_newTarget_exportsSnapshotWithDefaultName() {
        fail("TODO: Member D - D-22");
    }

    /**
     * D-23: overwrite declined (D8) [unit].
     *
     * <p>Arrange: existing target; confirm = false.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns false; no export request.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-23 overwrite declined (D8)")
    void run_existingTargetDeclined_exportsNothing() {
        fail("TODO: Member D - D-23");
    }

    /**
     * D-24: overwrite confirmed (D8) [unit].
     *
     * <p>Arrange: existing target; confirm = true.</p>
     * <p>Act: run.</p>
     * <p>Assert: export request present.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-24 overwrite confirmed (D8)")
    void run_existingTargetConfirmed_exports() {
        fail("TODO: Member D - D-24");
    }
}
